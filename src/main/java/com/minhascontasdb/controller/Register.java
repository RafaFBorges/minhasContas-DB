package com.minhascontasdb.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.RegisterRequestDTO;
import com.minhascontasdb.dto.RegisterResponseDTO;
import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.inicialization.UserInitialDataEnsurer;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.PasswordPolicy;
import com.minhascontasdb.service.User;

@RestController
@RequestMapping("/register")
public class Register {
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
    if (PasswordPolicy.isTooLong(password))
      throw new InvalidArgumentsError("Password too long");
    // Só no cadastro: contas antigas com senhas fracas continuam conseguindo entrar
    if (!PasswordPolicy.isStrong(password))
      throw new InvalidArgumentsError("Weak password");

    // Campos obrigatórios: sem isto a ausência só aparecia como erro do banco, na hora de salvar
    if (isBlank(userData.getUser()))
      throw new InvalidArgumentsError("Invalid user");
    if (isBlank(userData.getEmail()))
      throw new InvalidArgumentsError("Invalid email");

    if (userPersistence.existsByEmail(userData.getEmail()))
      throw new DuplicateDataError("Email já cadastrado.");

    // O nome da conta de serviço é reservado, em qualquer combinação de maiúsculas e minúsculas
    if (UserInitialDataEnsurer.isReserved(userData.getUser()))
      throw new DuplicateDataError("Nome de usuário já cadastrado.");

    if (userPersistence.existsByUser(userData.getUser()))
      throw new DuplicateDataError("Nome de usuário já cadastrado.");

    // O hash (caro de calcular) só é feito depois de todas as validações baratas
    User newUser = new User(userData.getName(), userData.getEmail(), passwordEncoder.encode(password), userData.getUser());

    User savedUser;
    try {
      savedUser = userPersistence.save(newUser);
    } catch (DataIntegrityViolationException e) {
      // Dois cadastros simultâneos passam juntos pelas checagens acima e o segundo bate na constraint unique do banco.
      // Isso é duplicidade (409), e o texto do banco não pode chegar ao cliente
      throw new DuplicateDataError("Usuário ou email já cadastrado.");
    }

    return ResponseEntity.ok(new RegisterResponseDTO(savedUser != null));
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
