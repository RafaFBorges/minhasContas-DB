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
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

@RestController
@CrossOrigin(origins = "*")
public class Login {
  static private final Map<String, UserSession> userSessions = new HashMap<>();

  @Autowired
  private UserPersistence userRepository;

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> tryLogin(@RequestBody LoginRequestDTO loginData) {
    UserSession userSession = Login.userSessions.get(loginData.getUser());

    if (userSession != null) {
      if (!userSession.isExpired())
        return ResponseEntity.ok(new LoginResponseDTO(userSession.token(), userSession.expiresAt()));

      userSessions.remove(loginData.getUser());
    }

    User user = userRepository.findByUser(loginData.getUser()).orElse(null);
    if (user != null &&
        user.getUser().equals(loginData.getUser()) &&
        user.getPassword().equals(loginData.getPassword())) {
      UserSession record = UserSession.create();
      Login.userSessions.put(loginData.getUser(), record);

      return ResponseEntity.ok(new LoginResponseDTO(record.token(), record.expiresAt()));
    }

    return ResponseEntity.status(401).build();
  }
}
