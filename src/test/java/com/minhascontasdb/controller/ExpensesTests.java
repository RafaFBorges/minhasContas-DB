package com.minhascontasdb.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.service.Expense;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class ExpensesTests {
  private static final Double DEFAULT_VALUE = 42.0;
  private static final Instant FIXED_INSTANT = LocalDateTime.of(2025, 9, 10, 7, 18, 0)
      .atZone(ZoneId.of("America/Sao_Paulo")).toInstant();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ExpensePersistence expensePersistence;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

  @Test
  void contextLoads() {
    // Teste básico para verificar se o contexto do Spring carrega corretamente.
  }

  @AfterEach
  void teardown() {
    expensePersistence.deleteAll();
  }

  @Nested
  class OnlyOneDataCase {
    private Expense savedExpense;

    @BeforeEach
    void setup() {
      this.savedExpense = expensePersistence.save(new Expense(DEFAULT_VALUE, FIXED_INSTANT));
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnRightExpenseWithID() throws Exception {
      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date", is(FIXED_INSTANT.toString())));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));
    }
  }

  @Nested
  class EmptyCase {
    @BeforeEach
    void setup() {
      // Deixado vazio de prropósito para não ter nenhum dado no DB
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturn404Error() throws Exception {
      mockMvc.perform(get("/expense/1"))
          .andDo(print())
          .andExpect(status().isNotFound());
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));
    }
  }

  @Nested
  class MultipleExpensesCase {
    private List<Expense> savedExpenses = new ArrayList<>();

    @BeforeEach
    void setup() {
      for (int i = 0; i < 3; i++)
        this.savedExpenses.add(expensePersistence
            .save(new Expense(DEFAULT_VALUE + 1, FIXED_INSTANT.plusSeconds(i))));
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnGetEachData() throws Exception {
      // foreach
      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.date", is(expense.getDate().toString())));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));
    }
  }
}
