package com.minhascontasdb.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.LoginRequestDTO;
import com.minhascontasdb.dto.LoginResponseDTO;
import com.minhascontasdb.dto.UserSession;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.dto.Errors.TooManyRequestsError;
import com.minhascontasdb.inicialization.UserInitialDataEnsurer;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.LoginRateLimiter;
import com.minhascontasdb.service.User;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "*")
public class Login {
  // Várias requisições registram e removem sessões ao mesmo tempo, então o mapa precisa ser thread-safe
  private static final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();

  private final UserPersistence userRepository;
  private final PasswordEncoder passwordEncoder;
  private final LoginRateLimiter rateLimiter;
  // Hash usado quando o usuário não existe, para o tempo de resposta não revelar quem está cadastrado
  private final String dummyHash;

  // Injeção de dependência via Construtor
  public Login(UserPersistence userRepository, PasswordEncoder passwordEncoder, LoginRateLimiter rateLimiter) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.rateLimiter = rateLimiter;
    this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
  }

  private boolean passwordMatches(String rawPassword, String storedHash) {
    if (rawPassword.getBytes(StandardCharsets.UTF_8).length > Register.MAX_PASSWORD_BYTES)
      return false;

    return passwordEncoder.matches(rawPassword, storedHash);
  }

  public static UserSession validateToken(String token) {
    boolean isValidToken = token != null && !token.isBlank() && Login.userSessions.containsKey(token);
    if (isValidToken) {
      UserSession session = Login.userSessions.get(token);
      isValidToken = session != null && !session.isExpired();

      if (isValidToken)
        return session;

      if (session != null)
        userSessions.remove(token);
    }

    throw new InvalidAccessError("Invalid token");
  }

  public static void registerSession(UserSession session) {
    if (session == null || session.token() == null || session.token().isBlank())
      return;

    Login.userSessions.put(session.token(), session);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> tryLogin(@RequestBody LoginRequestDTO loginData, HttpServletRequest request) {
    if (loginData.getUser() == null || loginData.getPassword() == null)
      return ResponseEntity.status(401).build();

    // A tentativa é reservada antes de qualquer consulta ou hash (e devolvida se o login der certo). Assim
    // requisições paralelas não passam todas pela checagem enquanto o hash é calculado.
    // O IP só serve ao limite por IP, que está desligado: atrás do proxy ele não é confiável (veja LoginRateLimiter)
    String clientIp = request.getRemoteAddr();
    long retryAfter = rateLimiter.reserveAttempt(loginData.getUser(), clientIp);
    if (0 < retryAfter)
      throw new TooManyRequestsError(retryAfter);

    User user = userRepository.findByUser(loginData.getUser()).orElse(null);
    boolean passwordMatches = passwordMatches(loginData.getPassword(), user != null ? user.getPassword() : this.dummyHash);

    // A conta System só é dona de dados compartilhados e nunca faz login
    boolean isSystemUser = user != null && UserInitialDataEnsurer.SYSTEM_USERNAME.equals(user.getUser());

    if (user != null && passwordMatches && !isSystemUser) {
      rateLimiter.releaseOnSuccess(loginData.getUser(), clientIp);

      UserSession record = UserSession.create(user.getId(), user.getUser(), user.getName());
      Login.registerSession(record);

      return ResponseEntity
          .ok(new LoginResponseDTO(
              record.token(),
              record.expiresAt(),
              user.getId(),
              user.getUser(),
              user.getName()));
    }

    return ResponseEntity.status(401).build();
  }
}
