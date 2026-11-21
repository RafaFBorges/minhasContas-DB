package com.minhascontasdb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.minhascontasdb.service.JwtService;
import com.minhascontasdb.service.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Quando a primeira regra de papel (@PreAuthorize/hasRole) existir, o AccessDeniedException lançado dentro de um controller não
// pode virar 500 no handler genérico
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class SecurityExceptionHandlingTests {
  @RestController
  static class ThrowingController {
    @GetMapping("/test-access-denied")
    String denied() {
      throw new AccessDeniedException("sem papel");
    }

    @GetMapping("/test-auth-failure")
    String authFailure() {
      throw new BadCredentialsException("credencial");
    }
  }

  @TestConfiguration
  static class Config {
    @Bean
    ThrowingController throwingController() {
      return new ThrowingController();
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtService jwtService;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  private String bearer() {
    User user = new User("Erro", "erro@test.com", "", "erro");
    user.setId(1L);

    return "Bearer " + jwtService.issue(user).value();
  }

  @Test
  void accessDeniedInsideAControllerIsForbidden() throws Exception {
    mockMvc.perform(get("/test-access-denied").header("Authorization", bearer()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Forbidden."));
  }

  @Test
  void authenticationFailureInsideAControllerIsUnauthorized() throws Exception {
    mockMvc.perform(get("/test-auth-failure").header("Authorization", bearer()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").exists());
  }
}
