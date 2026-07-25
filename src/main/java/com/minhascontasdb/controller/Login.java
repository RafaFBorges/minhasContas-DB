package com.minhascontasdb.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.LoginRequestDTO;
import com.minhascontasdb.dto.LoginResponseDTO;
import com.minhascontasdb.dto.UserSession;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

@RestController
@CrossOrigin(origins = "*")
public class Login {
  static private final Map<String, UserSession> userSessions = new HashMap<>();

  @Autowired
  private UserPersistence userRepository;

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
  public ResponseEntity<LoginResponseDTO> tryLogin(@RequestBody LoginRequestDTO loginData) {
    User user = userRepository.findByUser(loginData.getUser()).orElse(null);
    if (user != null &&
        user.getUser().equals(loginData.getUser()) &&
        user.getPassword().equals(loginData.getPassword())) {
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
