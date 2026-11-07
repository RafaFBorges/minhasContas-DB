package com.minhascontasdb.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.RegisterRequestDTO;
import com.minhascontasdb.dto.RegisterResponseDTO;
import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

@RestController
@RequestMapping("/register")
@CrossOrigin(origins = "*")
public class Register {

  // O BCrypt só considera os primeiros 72 bytes da senha
  public static final int MAX_PASSWORD_BYTES = 72;

  private final UserPersistence userPersistence;
  private final PasswordEncoder passwordEncoder;

  // Injeção de dependência via Construtor
  public Register(UserPersistence userPersistence, PasswordEncoder passwordEncoder) {
    this.userPersistence = userPersistence;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping
  public ResponseEntity<?> register(@RequestBody RegisterRequestDTO userData) {
    String password = userData.getPassword();
    if (password == null || password.isBlank())
      throw new InvalidArgumentsError("Invalid password");
    if (MAX_PASSWORD_BYTES < password.getBytes(StandardCharsets.UTF_8).length)
      throw new InvalidArgumentsError("Password too long");

    User newUser = new User(userData.getName(), userData.getEmail(), passwordEncoder.encode(password), userData.getUser());

    if (userPersistence.existsByEmail(newUser.getEmail()))
      throw new DuplicateDataError("Email já cadastrado.");

    if (userPersistence.existsByUser(newUser.getUser()))
      throw new DuplicateDataError("Nome de usuário já cadastrado.");

    User savedUser = userPersistence.save(newUser);

    return ResponseEntity.ok(new RegisterResponseDTO(savedUser != null));
  }
}
