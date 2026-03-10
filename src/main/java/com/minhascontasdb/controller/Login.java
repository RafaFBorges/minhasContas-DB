package com.minhascontasdb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.LoginRequestDTO;
import com.minhascontasdb.dto.LoginResponseDTO;

@RestController
@CrossOrigin(origins = "*")
public class Login {

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> tryLogin(@RequestBody LoginRequestDTO loginData) {
    LoginResponseDTO response = new LoginResponseDTO("TOKEN");

    return ResponseEntity.ok(response);
  }
}
