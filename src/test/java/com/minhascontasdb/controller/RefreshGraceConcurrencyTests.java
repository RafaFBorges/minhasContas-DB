package com.minhascontasdb.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhascontasdb.persistence.RefreshTokenPersistence;
import com.minhascontasdb.persistence.UserPersistence;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Com a tolerância ligada (como em produção), pedidos simultâneos com o mesmo refresh token recebem o MESMO sucessor
@SpringBootTest(properties = "jwt.refresh-reuse-grace-seconds=10")
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class RefreshGraceConcurrencyTests {
  private static final String PASSWORD = "S3nha-forte";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserPersistence userPersistence;

  @Autowired
  private RefreshTokenPersistence refreshTokenPersistence;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Test
  void parallelRefreshesShareASingleSuccessor() throws Exception {
    String username = "par-" + UUID.randomUUID().toString().substring(0, 8);
    User user = userPersistence.save(new User("Par", username + "@test.com", passwordEncoder.encode(PASSWORD), username));
    String login = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("user", username, "password", PASSWORD))))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    String refreshToken = objectMapper.readTree(login).get("refreshToken").asText();

    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<JsonNode>> results = new ArrayList<>();

    for (int i = 0; i < threads; i++)
      results.add(pool.submit(() -> {
        start.await();
        var response = mockMvc.perform(post("/refresh").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)))).andReturn().getResponse();
        assertEquals(200, response.getStatus());

        return objectMapper.readTree(response.getContentAsString());
      }));
    start.countDown();

    Set<String> successors = new java.util.HashSet<>();
    for (Future<JsonNode> r : results)
      successors.add(r.get().get("refreshToken").asText());
    pool.shutdown();

    assertEquals(1, successors.size());
    // Uma linha trocada e um único sucessor ativo
    List<com.minhascontasdb.service.RefreshToken> rows = refreshTokenPersistence.findAll().stream()
        .filter(t -> t.getUserId().equals(user.getId())).collect(Collectors.toList());
    assertEquals(2, rows.size());
    assertEquals(1, rows.stream().filter(t -> t.getRevokedAt() == null).count());
  }
}
