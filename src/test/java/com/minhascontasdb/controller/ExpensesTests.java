package com.minhascontasdb.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
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

    // Teste para o endpoint GET /expense/{id}
    @Test
    void getExpenseId_ShouldReturnRightExpenseWithID() throws Exception {
      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.dates[-1]", is(FIXED_INSTANT.toString())));
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnAllExpenses() throws Exception {
      mockMvc.perform(get("/expense"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$[0].dates[-1]", is(FIXED_INSTANT.toString())));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.dates[-1]", is(requestDTO.getDate().toString())));
    }

    // Teste para endpoint PUT /expense
    @Test
    void putExpense_ShouldEditTheValue() throws Exception {
      // Verificar que o valor esta no banco de dados
      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.dates[-1]", is(FIXED_INSTANT.toString())));

      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE * 2,
          FIXED_INSTANT.plusSeconds(100).truncatedTo(ChronoUnit.SECONDS));
      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(put("/expense/{id}", this.savedExpense.getId())
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE * 2)))
          .andExpect(jsonPath("$.dates[-1]", is(requestDTO.getDate().toString())));

      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE * 2)))
          .andExpect(jsonPath("$.dates[-1]", is(requestDTO.getDate().toString())));
    }

    // Teste para endpoint DELETE /expense
    @Test
    void deleteExpense_ShouldReturnNoContentAndRemoveExpense() throws Exception {
      mockMvc.perform(delete("/expense/{id}", savedExpense.getId()))
          .andExpect(status().isNoContent());

      mockMvc.perform(get("/expense/{id}", savedExpense.getId()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class EmptyCase {
    @BeforeEach
    void setup() {
      // Deixado vazio de prropósito para não ter nenhum dado no DB
    }

    // Teste para o endpoint GET /expense/{id}
    @Test
    void getExpenseId_ShouldReturn404Error() throws Exception {
      mockMvc.perform(get("/expense/1"))
          .andExpect(status().isNotFound());
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnEmptyList() throws Exception {
      mockMvc.perform(get("/expense"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.dates[-1]", is(requestDTO.getDate().toString())));
    }

    // Teste para endpoint PUT /expense
    @Test
    void putExpense_ShouldReturn404Error() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE * 2, Instant.now());
      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(put("/expense/1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isNotFound());
    }

    // Teste para endpoint DELETE /expense
    @Test
    void deleteExpense_ShouldReturnNoContent() throws Exception {
      mockMvc.perform(delete("/expense/1"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class MultipleExpensesCase {
    private final Integer LIST_COUNT = 3;
    private List<Expense> savedExpenses = new ArrayList<>();

    @BeforeEach
    void setup() {
      for (int i = 0; i < this.LIST_COUNT; i++)
        this.savedExpenses.add(expensePersistence
            .save(new Expense(DEFAULT_VALUE + 1, FIXED_INSTANT.plusSeconds(i))));
    }

    // Teste para o endpoint GET /expense/{id}
    @Test
    void getExpenseId_ShouldReturnGetEachData() throws Exception {
      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.dates[-1]", is(expense.getLastDate().toString())));
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnAllExpenses() throws Exception {
      mockMvc.perform(get("/expense"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(this.LIST_COUNT)))
          .andExpect(jsonPath("$[0].value", is(savedExpenses.get(0).getValue())))
          .andExpect(jsonPath("$[0].dates[-1]", is(savedExpenses.get(0).getLastDate().toString())))
          .andExpect(jsonPath("$[1].value", is(savedExpenses.get(0).getValue())))
          .andExpect(jsonPath("$[1].dates[-1]", is(savedExpenses.get(1).getLastDate().toString())))
          .andExpect(jsonPath("$[2].value", is(savedExpenses.get(0).getValue())))
          .andExpect(jsonPath("$[2].dates[-1]", is(savedExpenses.get(2).getLastDate().toString())));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());
      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.dates[-1]", is(requestDTO.getDate().toString())));
    }

    // Teste para endpoint PUT /expense
    @Test
    void putExpense_ShouldEditTheValue() throws Exception {
      // Verificar que o valor esta no banco de dados
      final Integer UPDATE_INDEX = 1;

      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.dates[-1]", is(expense.getLastDate().toString())));

      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE * 2,
          FIXED_INSTANT.plusSeconds(100).truncatedTo(ChronoUnit.SECONDS));
      String requestJson = objectMapper.writeValueAsString(requestDTO);
      savedExpenses.get(UPDATE_INDEX).setValue(requestDTO.getValue(), requestDTO.getDate());

      mockMvc.perform(put("/expense/{id}", savedExpenses.get(UPDATE_INDEX).getId())
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE * 2)))
          .andExpect(jsonPath("$.dates[-1]", is(requestDTO.getDate().toString())));

      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.dates[-1]", is(expense.getLastDate().toString())));
    }

    // Teste para endpoint DELETE /expense
    @Test
    void deleteExpense_ShouldReturnNoContentAndRemoveExpense() throws Exception {
      final Integer DELETE_INDEX = 1;

      mockMvc.perform(delete("/expense/{id}", savedExpenses.get(DELETE_INDEX).getId()))
          .andExpect(status().isNoContent());

      mockMvc.perform(get("/expense/{id}", savedExpenses.get(DELETE_INDEX).getId()))
          .andExpect(status().isNotFound());

      savedExpenses.remove(savedExpenses.get(DELETE_INDEX));
      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.dates[-1]", is(expense.getLastDate().toString())));
    }
  }
}
