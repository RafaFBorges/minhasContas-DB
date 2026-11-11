package com.minhascontasdb.controller;

import com.minhascontasdb.filter.HstsFilter;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class TransportSecurityTests {
  @Autowired
  private MockMvc mockMvc;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Test
  void secureRequestReceivesHsts() throws Exception {
    mockMvc.perform(get("/").secure(true))
        .andExpect(status().isOk())
        .andExpect(header().string(HstsFilter.HEADER, HstsFilter.VALUE));
  }

  @Test
  void plainHttpRequestDoesNotReceiveHsts() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HstsFilter.HEADER));
  }

  // Atrás do proxy a conexão com a API é HTTP, e quem informa o HTTPS do cliente é o X-Forwarded-Proto
  @Test
  void forwardedHttpsFromProxyReceivesHsts() throws Exception {
    mockMvc.perform(get("/").header("X-Forwarded-Proto", "https"))
        .andExpect(status().isOk())
        .andExpect(header().string(HstsFilter.HEADER, HstsFilter.VALUE));
  }

  @Test
  void forwardedHttpFromProxyDoesNotReceiveHsts() throws Exception {
    mockMvc.perform(get("/").header("X-Forwarded-Proto", "http"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HstsFilter.HEADER));
  }

  // O filtro roda antes do controller: o HSTS também precisa sair nas respostas de erro
  @Test
  void secureErrorResponsesAlsoReceiveHsts() throws Exception {
    mockMvc.perform(post("/login")
        .secure(true)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"user\":\"ninguem\",\"password\":\"qualquer\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HstsFilter.HEADER, HstsFilter.VALUE));

    // Rota autenticada sem o header "token"
    mockMvc.perform(get("/expense/1").secure(true))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HstsFilter.HEADER, HstsFilter.VALUE));
  }
}
