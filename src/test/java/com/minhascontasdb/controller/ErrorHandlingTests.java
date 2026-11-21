package com.minhascontasdb.controller;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.service.JwtService;
import com.minhascontasdb.service.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class ErrorHandlingTests {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtService jwtService;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  // Access token válido sem precisar de usuário no banco: as rotas abaixo falham antes de consultar dados
  private String bearer() {
    User user = new User("Erro", "erro@test.com", "", "erro");
    user.setId(1L);

    return "Bearer " + jwtService.issue(user).value();
  }

  // Erros do próprio Spring MVC mantêm o status certo (antes viravam 500 no handler genérico)
  @Test
  void unknownRouteIsNotFound() throws Exception {
    mockMvc.perform(get("/rota-que-nao-existe").header("Authorization", bearer()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Not Found"));
  }

  @Test
  void wrongHttpMethodIsMethodNotAllowed() throws Exception {
    mockMvc.perform(get("/login"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void unsupportedContentTypeIsUnsupportedMediaType() throws Exception {
    mockMvc.perform(post("/login").contentType(MediaType.TEXT_PLAIN).content("user=a"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void malformedJsonIsBadRequest() throws Exception {
    mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed request body"));
  }

  @Test
  void pathParameterWithWrongTypeIsBadRequest() throws Exception {
    mockMvc.perform(get("/expense/abc").header("Authorization", bearer()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  // Erro inesperado de verdade continua 500, mas sem expor a mensagem da exceção
  @Test
  void unexpectedErrorIsInternalServerErrorWithoutLeakingDetails() {
    ResponseEntity<ErrorResponseDTO> response = new GlobalExceptionHandler()
        .handleGeneric(new RuntimeException("select * from users where password='segredo'"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Internal server error", response.getBody().getMessage());
    assertFalse(response.getBody().getMessage().contains("segredo"));
  }
}
