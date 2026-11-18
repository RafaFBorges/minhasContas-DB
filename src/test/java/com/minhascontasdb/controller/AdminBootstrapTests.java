package com.minhascontasdb.controller;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhascontasdb.inicialization.AdminInitialDataEnsurer;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Role;
import com.minhascontasdb.service.User;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sobe a aplicação inteira com ADMIN_* configurados
@SpringBootTest(properties = {
    "admin.user=boot-admin",
    "admin.email=boot-admin@test.com",
    "admin.password=S3nha-forte-do-admin"
})
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class AdminBootstrapTests {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserPersistence userPersistence;

  @Autowired
  private AdminInitialDataEnsurer ensurer;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Test
  void adminIsCreatedOnStartupCanLoginAndGetsTheAdminRole() throws Exception {
    User admin = userPersistence.findByUser("boot-admin").orElseThrow();
    assertEquals(Role.ADMIN, admin.getRole());

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("user", "boot-admin", "password", "S3nha-forte-do-admin"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void runningAgainDoesNotCreateAnotherAdminOrChangeTheExistingOne() {
    String hash = userPersistence.findByUser("boot-admin").orElseThrow().getPassword();

    ensurer.run();

    assertEquals(hash, userPersistence.findByUser("boot-admin").orElseThrow().getPassword());
    assertEquals(1, userPersistence.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).count());
  }

  @Test
  void systemAccountHasTheSystemRoleAndNewRegistrationsAreUsers() {
    assertEquals(Role.SYSTEM, userPersistence.findByUser("System").orElseThrow().getRole());
  }
}
