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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem CORS_ALLOWED_ORIGINS (o padrão em produção) nenhuma origem de navegador é liberada: o Web chama a API pelo servidor,
// o que não passa por CORS
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class CorsTests {
  private static final String OTHER_SITE = "https://outro-site.com";

  @Autowired
  private MockMvc mockMvc;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Test
  void crossOriginRequestDoesNotReceiveAllowOrigin() throws Exception {
    mockMvc.perform(get("/").header(HttpHeaders.ORIGIN, OTHER_SITE))
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void preflightDoesNotReceiveAllowOrigin() throws Exception {
    mockMvc.perform(options("/expense")
        .header(HttpHeaders.ORIGIN, OTHER_SITE)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
  }

  // Chamada sem Origin (o caso do servidor do Web) segue funcionando normalmente
  @Test
  void requestWithoutOriginStillWorks() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk());
  }
}
