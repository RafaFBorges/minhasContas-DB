package com.minhascontasdb.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.minhascontasdb.dto.RegisterRequestDTO;
import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.UserPersistence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Sem Spring nem banco: o que se quer ver é a ordem das checagens e a conversão do erro do banco, que um teste de integração
// não consegue provocar de forma confiável (é preciso dois cadastros passando juntos pelas checagens)
public class RegisterTests {
  private static final String PASSWORD = "Abcdef1!ghij";

  private UserPersistence users;
  private Register register;

  @BeforeEach
  void setUp() {
    users = mock(UserPersistence.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(encoder.encode(any())).thenReturn("hash");

    register = new Register(users, encoder);
  }

  private RegisterRequestDTO request(String user, String email) {
    // Construtor: (name, email, password, user)
    return new RegisterRequestDTO("Nome", email, PASSWORD, user);
  }

  @Test
  void missingOrBlankUserIsRejectedBeforeAnyLookupOrSave() {
    assertThrows(InvalidArgumentsError.class, () -> register.register(request(null, "a@x.com")));
    assertThrows(InvalidArgumentsError.class, () -> register.register(request("   ", "a@x.com")));

    verify(users, never()).save(any());
  }

  @Test
  void missingOrBlankEmailIsRejectedBeforeAnyLookupOrSave() {
    assertThrows(InvalidArgumentsError.class, () -> register.register(request("ana", null)));
    assertThrows(InvalidArgumentsError.class, () -> register.register(request("ana", "")));

    verify(users, never()).existsByEmail(any());
    verify(users, never()).save(any());
  }

  // Dois cadastros iguais passam juntos pelas checagens (nenhum existe ainda) e o segundo bate na constraint unique
  @Test
  void uniqueConstraintViolationOnSaveBecomesDuplicateDataNotADatabaseMessage() {
    when(users.existsByEmail("a@x.com")).thenReturn(false);
    when(users.existsByUser("ana")).thenReturn(false);
    when(users.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_username_key\""));

    assertThrows(DuplicateDataError.class, () -> register.register(request("ana", "a@x.com")));
  }
}
