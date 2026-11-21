package com.minhascontasdb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.LoginRequestDTO;
import com.minhascontasdb.dto.LoginResponseDTO;
import com.minhascontasdb.dto.RefreshRequestDTO;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.dto.Errors.TooManyRequestsError;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.JwtService;
import com.minhascontasdb.service.LoginRateLimiter;
import com.minhascontasdb.service.PasswordPolicy;
import com.minhascontasdb.service.RefreshTokenService;
import com.minhascontasdb.service.User;

import jakarta.servlet.http.HttpServletRequest;

// Login, renovação e logout. O access token (JWT) dura pouco e não é revogável; a sessão de verdade é o refresh token
@RestController
@CrossOrigin(origins = "*")
public class Auth {
  private final UserPersistence userRepository;
  private final PasswordEncoder passwordEncoder;
  private final LoginRateLimiter rateLimiter;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokens;
  // Hash usado quando o usuário não existe, para o tempo de resposta não revelar quem está cadastrado
  private final String dummyHash;

  // Injeção de dependência via Construtor
  public Auth(UserPersistence userRepository, PasswordEncoder passwordEncoder, LoginRateLimiter rateLimiter,
      JwtService jwtService, RefreshTokenService refreshTokens) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.rateLimiter = rateLimiter;
    this.jwtService = jwtService;
    this.refreshTokens = refreshTokens;
    this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
  }

  private boolean passwordMatches(String rawPassword, String storedHash) {
    if (PasswordPolicy.isTooLong(rawPassword))
      return false;

    return passwordEncoder.matches(rawPassword, storedHash);
  }

  private LoginResponseDTO tokensFor(User user, RefreshTokenService.Issued refresh) {
    JwtService.AccessToken access = jwtService.issue(user);

    return new LoginResponseDTO(
        access.value(),
        access.expiresAt(),
        refresh.token(),
        refresh.expiresAt(),
        user.getId(),
        user.getUser(),
        user.getName(),
        user.getRole());
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
    boolean isSystemUser = user != null && user.isSystem();

    if (user != null && passwordMatches && !isSystemUser) {
      rateLimiter.releaseOnSuccess(loginData.getUser(), clientIp);

      return ResponseEntity.ok(tokensFor(user, refreshTokens.issue(user.getId())));
    }

    return ResponseEntity.status(401).build();
  }

  // Troca o refresh token por um par novo. O refresh token enviado deixa de valer: guarde sempre o novo
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponseDTO> refresh(@RequestBody RefreshRequestDTO body) {
    RefreshTokenService.Rotation rotation = refreshTokens.rotate(body.refreshToken())
        .orElseThrow(() -> new InvalidAccessError("Invalid refresh token"));

    return ResponseEntity.ok(tokensFor(rotation.user(), rotation.refresh()));
  }

  // Encerra a sessão. O access token já emitido segue válido até vencer (no máximo 15 min)
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody RefreshRequestDTO body) {
    refreshTokens.revoke(body.refreshToken());

    return ResponseEntity.noContent().build();
  }
}
