package com.minhascontasdb.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.LoginRequestDTO;
import com.minhascontasdb.dto.LoginResponseDTO;
import com.minhascontasdb.dto.UserSession;

@RestController
@CrossOrigin(origins = "*")
public class Login {
  static private final Map<String, UserSession> userSessions = new HashMap<>();

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> tryLogin(@RequestBody LoginRequestDTO loginData) {
    UserSession user = Login.userSessions.get(loginData.getUser());

    if (user != null) {
      if (!user.isExpired())
        return ResponseEntity.ok(new LoginResponseDTO(user.token(), user.expiresAt()));

      userSessions.remove(loginData.getUser());
    }

    if ("rfborges".equals(loginData.getUser()) && "1234".equals(loginData.getPassword())) {
      UserSession record = UserSession.create();
      Login.userSessions.put(loginData.getUser(), record);

      return ResponseEntity.ok(new LoginResponseDTO(record.token(), record.expiresAt()));
    }

    return ResponseEntity.status(401).build();
  }
}
