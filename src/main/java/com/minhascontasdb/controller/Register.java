package com.minhascontasdb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.RegisterRequestDTO;
import com.minhascontasdb.dto.RegisterResponseDTO;
import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

@RestController
@RequestMapping("/register")
@CrossOrigin(origins = "*")
public class Register {

  private final UserPersistence userPersistence;

  // Injeção de dependência via Construtor
  public Register(UserPersistence userPersistence) {
    this.userPersistence = userPersistence;
  }

  @PostMapping
  public ResponseEntity<?> register(@RequestBody RegisterRequestDTO userData) {
    User newUser = new User(userData.getName(), userData.getEmail(), userData.getPassword(), userData.getUser());

    if (userPersistence.existsByEmail(newUser.getEmail()))
      throw new DuplicateDataError("Email já cadastrado.");

    if (userPersistence.existsByUser(newUser.getUser()))
      throw new DuplicateDataError("Nome de usuário já cadastrado.");

    User savedUser = userPersistence.save(newUser);

    return ResponseEntity.ok(new RegisterResponseDTO(savedUser != null));
  }
}
