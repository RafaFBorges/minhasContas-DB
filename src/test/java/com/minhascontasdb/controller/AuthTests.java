package com.minhascontasdb.controller;

import java.util.Map;

import com.minhascontasdb.inicialization.UserInitialDataEnsurer;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class AuthTests {
  private static final String PASSWORD = "S3nha-forte";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserPersistence userPersistence;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  private int register(String username, String password) throws Exception {
    Map<String, String> body = Map.of(
        "name", "Auth " + username,
        "email", username + "@test.com",
        "password", password,
        "user", username);

    return mockMvc.perform(post("/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)))
        .andReturn().getResponse().getStatus();
  }

  private int login(String username, String password) throws Exception {
    Map<String, String> body = Map.of("user", username, "password", password);

    return mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)))
        .andReturn().getResponse().getStatus();
  }

  @Test
  void registerStoresOnlyTheHashOfThePassword() throws Exception {
    assertEquals(200, register("hashed", PASSWORD));

    User saved = userPersistence.findByUser("hashed").orElseThrow();
    assertNotEquals(PASSWORD, saved.getPassword());
    assertTrue(saved.getPassword().startsWith("$2"));
    assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
  }

  @Test
  void loginWithCorrectPasswordReturnsToken() throws Exception {
    register("goodlogin", PASSWORD);

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("user", "goodlogin", "password", PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void loginWithWrongPasswordIsUnauthorized() throws Exception {
    register("wrongpass", PASSWORD);

    assertEquals(401, login("wrongpass", "outra-senha"));
  }

  @Test
  void loginWithUnknownUserIsUnauthorized() throws Exception {
    assertEquals(401, login("ninguem", PASSWORD));
  }

  @Test
  void loginWithoutPasswordIsUnauthorized() throws Exception {
    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"user\":\"hashed\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerRejectsBlankAndTooLongPasswords() throws Exception {
    assertEquals(400, register("blankpass", "   "));
    assertEquals(400, register("longpass", "a".repeat(Register.MAX_PASSWORD_BYTES + 1)));
    assertFalse(userPersistence.existsByUser("blankpass"));
    assertFalse(userPersistence.existsByUser("longpass"));
  }

  @Test
  void loginWithTooLongPasswordIsUnauthorized() throws Exception {
    register("shortpass", PASSWORD);

    assertEquals(401, login("shortpass", "a".repeat(Register.MAX_PASSWORD_BYTES + 1)));
  }

  @Test
  void systemUsernameIsReservedInAnyLetterCase() throws Exception {
    assertEquals(409, register("System", PASSWORD));
    assertEquals(409, register("system", PASSWORD));
    assertEquals(409, register("SYSTEM", PASSWORD));

    // a conta de serviço continua sendo a única com esse nome e sem a senha informada
    User system = userPersistence.findByUser(UserInitialDataEnsurer.SYSTEM_USERNAME).orElseThrow();
    assertFalse(passwordEncoder.matches(PASSWORD, system.getPassword()));
    assertFalse(userPersistence.existsByUser("system"));
  }

  @Test
  void systemUserHasRandomPasswordAndCannotLogin() throws Exception {
    User system = userPersistence.findByUser(UserInitialDataEnsurer.SYSTEM_USERNAME).orElseThrow();

    // Nem a senha antiga ("system") entra
    assertFalse(passwordEncoder.matches("system", system.getPassword()));
    assertEquals(401, login(UserInitialDataEnsurer.SYSTEM_USERNAME, "system"));

    // Mesmo que alguém defina uma senha conhecida, a conta System é recusada
    String originalHash = system.getPassword();
    try {
      system.setPassword(passwordEncoder.encode(PASSWORD));
      userPersistence.save(system);

      assertEquals(401, login(UserInitialDataEnsurer.SYSTEM_USERNAME, PASSWORD));
    } finally {
      // Restaura a senha aleatória para este teste não afetar os outros, qualquer que seja a ordem
      system.setPassword(originalHash);
      userPersistence.save(system);
    }
  }
}
