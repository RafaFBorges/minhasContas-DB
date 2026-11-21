package com.minhascontasdb.inicialization;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Role;
import com.minhascontasdb.service.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInitialDataEnsurerTests {
  private static final String PASSWORD = "S3nha-forte-do-admin";

  private final UserPersistence users = mock(UserPersistence.class);
  private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

  private AdminInitialDataEnsurer ensurer(String user, String email, String password) {
    return new AdminInitialDataEnsurer(users, encoder, user, email, password);
  }

  @Test
  void createsTheAdminWithHashedPasswordAndAdminRole() {
    ensurer("admin", "admin@x.com", PASSWORD).run();

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertEquals(Role.ADMIN, saved.getValue().getRole());
    assertEquals("admin", saved.getValue().getUser());
    assertEquals("admin@x.com", saved.getValue().getEmail());
    assertTrue(encoder.matches(PASSWORD, saved.getValue().getPassword()));
    assertFalse(saved.getValue().getPassword().contains(PASSWORD));
  }

  @Test
  void doesNothingWhenAnAdminAlreadyExistsEvenWithInvalidConfiguration() {
    when(users.existsByRole(Role.ADMIN)).thenReturn(true);

    ensurer("admin", "admin@x.com", "fraca").run();

    verify(users, never()).save(any());
  }

  @Test
  void doesNothingWhenNothingIsConfigured() {
    ensurer("", "", "").run();
    ensurer(null, null, null).run();

    verify(users, never()).save(any());
  }

  @Test
  void partialConfigurationFailsTheStartup() {
    assertThrows(IllegalStateException.class, () -> ensurer("admin", "", PASSWORD).run());
    assertThrows(IllegalStateException.class, () -> ensurer("", "a@x.com", PASSWORD).run());
    assertThrows(IllegalStateException.class, () -> ensurer("admin", "a@x.com", "").run());
    verify(users, never()).save(any());
  }

  @Test
  void weakOrTooLongPasswordFailsWithoutLeakingIt() {
    for (String weak : new String[] { "fraca", "semmaiuscula1!", "SEMMINUSCULA1!", "SemNumero!!", "SemEspecial12" }) {
      IllegalStateException e = assertThrows(IllegalStateException.class, () -> ensurer("admin", "a@x.com", weak).run());
      assertFalse(e.getMessage().contains(weak));
    }
    assertThrows(IllegalStateException.class, () -> ensurer("admin", "a@x.com", "Aa1!" + "x".repeat(80)).run());
    verify(users, never()).save(any());
  }

  @Test
  void reservedIdentityFailsTheStartup() {
    assertThrows(IllegalStateException.class, () -> ensurer("SYSTEM", "a@x.com", PASSWORD).run());
    verify(users, never()).save(any());
  }

  // Admin rebaixado com as variáveis ainda no ambiente: a API tem que subir, sem criar nada
  @Test
  void identityUsedByAnotherAccountSkipsTheBootstrapWithoutFailing() {
    when(users.existsByUser("admin")).thenReturn(true);
    ensurer("admin", "a@x.com", PASSWORD).run();

    when(users.existsByUser("admin")).thenReturn(false);
    when(users.existsByEmail("a@x.com")).thenReturn(true);
    ensurer("admin", "a@x.com", PASSWORD).run();

    verify(users, never()).save(any());
  }

  // Duas instâncias subindo juntas: a que perde a corrida na chave única segue em frente se o Admin já existe
  @Test
  void duplicateKeyFromAConcurrentInstanceIsToleratedWhenTheAdminNowExists() {
    when(users.existsByRole(Role.ADMIN)).thenReturn(false, true);
    when(users.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

    ensurer("admin", "a@x.com", PASSWORD).run();
  }

  @Test
  void duplicateKeyWithoutAnAdminIsStillAnError() {
    when(users.existsByRole(Role.ADMIN)).thenReturn(false);
    when(users.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThrows(DataIntegrityViolationException.class, () -> ensurer("admin", "a@x.com", PASSWORD).run());
  }
}
