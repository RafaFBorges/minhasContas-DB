package com.minhascontasdb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Com CORS_ALLOWED_ORIGINS definida (ex.: um front local), só as origens da lista passam
@SpringBootTest(properties = "cors.allowed-origins=https://app.exemplo.com, http://localhost:3000")
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class CorsAllowedOriginTests {
  @Autowired
  private MockMvc mockMvc;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Test
  void listedOriginReceivesAllowOrigin() throws Exception {
    mockMvc.perform(get("/").header(HttpHeaders.ORIGIN, "https://app.exemplo.com"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.exemplo.com"));
  }

  // Sem isto o front, numa origem liberada, leria Retry-After como null no 429 do login
  @Test
  void retryAfterAndWwwAuthenticateAreExposedToTheBrowser() throws Exception {
    mockMvc.perform(get("/").header(HttpHeaders.ORIGIN, "https://app.exemplo.com"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Retry-After")))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("WWW-Authenticate")));
  }

  @Test
  void secondListedOriginAlsoReceivesAllowOrigin() throws Exception {
    mockMvc.perform(get("/").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
  }

  @Test
  void unlistedOriginIsRefused() throws Exception {
    mockMvc.perform(get("/").header(HttpHeaders.ORIGIN, "https://outro-site.com"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void preflightFromListedOriginAllowsAuthorizationAndContentType() throws Exception {
    mockMvc.perform(options("/expense")
        .header(HttpHeaders.ORIGIN, "https://app.exemplo.com")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.exemplo.com"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("authorization")))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("content-type")));
  }

  // O cabeçalho "token" era do contrato antigo (antes do Bearer): o preflight que o peça não é autorizado
  @Test
  void preflightRequestingAnUnlistedHeaderIsRefused() throws Exception {
    mockMvc.perform(options("/expense")
        .header(HttpHeaders.ORIGIN, "https://app.exemplo.com")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "token"))
        .andExpect(status().isForbidden());
  }
}
