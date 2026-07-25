package com.minhascontasdb.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.dto.LoginRequestDTO;
import com.minhascontasdb.dto.LoginResponseDTO;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class ExpensesTests {
  private static final Double DEFAULT_VALUE = 42.0;
  private static final Instant FIXED_INSTANT = LocalDateTime.of(2025, 9, 10, 7, 18, 0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ExpensePersistence expensePersistence;

  @Autowired
  private CategoryPersistence categoryRepository;

  @Autowired
  private UserPersistence userPersistence;
  private User testUser;

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
    categoryRepository.deleteAll();
    userPersistence.deleteAll();
  }

  @Nested
  class OnlyOneDataCase {
    private Expense savedExpense;
    private String authToken;

    @BeforeEach
    void setup() throws Exception {
      // Save user to database with known credentials
      testUser = userPersistence.save(new User("Test User", "test@test.com", "password", "testuser"));
      
      // Perform login to obtain dynamic session token
      LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "password");
      String loginJson = objectMapper.writeValueAsString(loginRequest);

      String response = mockMvc.perform(post("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

      // Extract token from response JSON
      LoginResponseDTO loginResponse = objectMapper.readValue(response, LoginResponseDTO.class);
      this.authToken = loginResponse.getToken();

      // Save initial expense
      Expense expense = new Expense(DEFAULT_VALUE, FIXED_INSTANT);
      expense.setOwner(testUser);
      this.savedExpense = expensePersistence.save(expense);
    }

    // Teste para o endpoint GET /expense/{id}
    @Test
    void getExpenseId_ShouldReturnRightExpenseWithID() throws Exception {
      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId())
          .header("token", this.authToken))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date", is(FIXED_INSTANT.toString())));
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnAllExpenses() throws Exception {
      mockMvc.perform(get("/expense/user/{id}", testUser.getId())
          .header("token", this.authToken))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$[0].date", is(FIXED_INSTANT.toString())));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());
      requestDTO.setOwner(testUser.getId());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .header("token", this.authToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date").exists());
    }

    // Teste para endpoint PUT /expense
    @Test
    void putExpense_ShouldEditTheValue() throws Exception {
      // Verificar que o valor esta no banco de dados
      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId())
          .header("token", this.authToken))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date", is(FIXED_INSTANT.toString())));

      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE * 2, FIXED_INSTANT.plusSeconds(100).truncatedTo(ChronoUnit.SECONDS));
      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(put("/expense/{id}", this.savedExpense.getId())
          .header("token", this.authToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE * 2)))
          .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));

      mockMvc.perform(get("/expense/{id}", this.savedExpense.getId())
          .header("token", this.authToken))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE * 2)))
          .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));
    }

    // Teste para endpoint DELETE /expense
    @Test
    void deleteExpense_ShouldReturnNoContentAndRemoveExpense() throws Exception {
      mockMvc.perform(delete("/expense/{id}", savedExpense.getId())
          .header("token", this.authToken))
          .andExpect(status().isNoContent());

      mockMvc.perform(get("/expense/{id}", savedExpense.getId())
          .header("token", this.authToken))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class EmptyCase {
    private String authToken;
    private User testUser;

    @BeforeEach
    void setup() throws Exception {
      // Create and save user to generate token, but KEEP EXPENSES DATABASE EMPTY
      testUser = userPersistence.save(new User("Empty User", "empty@test.com", "password", "emptyuser"));

      LoginRequestDTO loginRequest = new LoginRequestDTO("emptyuser", "password");
      String loginJson = objectMapper.writeValueAsString(loginRequest);

      String response = mockMvc.perform(post("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

      LoginResponseDTO loginResponse = objectMapper.readValue(response, LoginResponseDTO.class);
      this.authToken = loginResponse.getToken();
    }

    // Teste para o endpoint GET /expense/{id}
    @Test
    void getExpenseId_ShouldReturn404Error() throws Exception {
      mockMvc.perform(get("/expense/1")
          .header("token", this.authToken))
          .andExpect(status().isNotFound());
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnEmptyList() throws Exception {
      mockMvc.perform(get("/expense/user/{id}", this.testUser.getId())
          .header("token", this.authToken))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());
      requestDTO.setOwner(this.testUser.getId());

      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .header("token", this.authToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date").exists());
    }

    // Teste para endpoint PUT /expense
    @Test
    void putExpense_ShouldReturn404Error() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE * 2, Instant.now());
      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(put("/expense/1")
          .header("token", this.authToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isNotFound());
    }

    // Teste para endpoint DELETE /expense
    @Test
    void deleteExpense_ShouldReturnNoContent() throws Exception {
      mockMvc.perform(delete("/expense/1")
          .header("token", this.authToken))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class MultipleExpensesCase {
    private final Integer LIST_COUNT = 3;
    private List<Expense> savedExpenses = new ArrayList<>();
    private String authToken;
    private User testUser;

    @BeforeEach
    void setup() throws Exception {
      savedExpenses.clear();

      // Create user and log in to obtain token
      testUser = userPersistence.save(new User("Multi User", "multi@test.com", "password", "multiuser"));

      LoginRequestDTO loginRequest = new LoginRequestDTO("multiuser", "password");
      String loginJson = objectMapper.writeValueAsString(loginRequest);

      String response = mockMvc.perform(post("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

      LoginResponseDTO loginResponse = objectMapper.readValue(response, LoginResponseDTO.class);
      this.authToken = loginResponse.getToken();

      // Create expenses associated with the user
      for (int i = 0; i < this.LIST_COUNT; i++) {
        Expense exp = new Expense(DEFAULT_VALUE + i, FIXED_INSTANT.plusSeconds(i).truncatedTo(ChronoUnit.SECONDS));
        exp.setOwner(testUser);
        this.savedExpenses.add(expensePersistence.save(exp));
      }
    }

    // Teste para o endpoint GET /expense/{id}
    @Test
    void getExpenseId_ShouldReturnGetEachData() throws Exception {
      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId())
            .header("token", this.authToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.date", is(expense.getLastDate().toString())));
    }

    // Teste para o endpoint GET /expense
    @Test
    void getExpense_ShouldReturnAllExpenses() throws Exception {
      mockMvc.perform(get("/expense/user/{id}", this.testUser.getId())
          .header("token", this.authToken))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(this.LIST_COUNT)))
          .andExpect(jsonPath("$[0].value", is(savedExpenses.get(0).getValue())))
          .andExpect(jsonPath("$[0].date").exists())
          .andExpect(jsonPath("$[1].value", is(savedExpenses.get(1).getValue())))
          .andExpect(jsonPath("$[1].date").exists())
          .andExpect(jsonPath("$[2].value", is(savedExpenses.get(2).getValue())))
          .andExpect(jsonPath("$[2].date").exists());
    }

    // Teste para o endpoint POST /expense
    @Test
    void createExpense_ShouldReturnCreatedExpense() throws Exception {
      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE, Instant.now());
      requestDTO.setOwner(this.testUser.getId());
      
      String requestJson = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(post("/expense")
          .header("token", this.authToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE)))
          .andExpect(jsonPath("$.date").exists());
    }

    // Teste para endpoint PUT /expense
    @Test
    void putExpense_ShouldEditTheValue() throws Exception {
      // Verificar que o valor esta no banco de dados
      final Integer UPDATE_INDEX = 1;

      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId())
            .header("token", this.authToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.date", is(expense.getLastDate().toString())));

      ExpenseRequestDTO requestDTO = new ExpenseRequestDTO(DEFAULT_VALUE * 2, FIXED_INSTANT.plusSeconds(100).truncatedTo(ChronoUnit.SECONDS));
      String requestJson = objectMapper.writeValueAsString(requestDTO);
      savedExpenses.get(UPDATE_INDEX).setValue(requestDTO.getValue(), requestDTO.getDate());

      mockMvc.perform(put("/expense/{id}", savedExpenses.get(UPDATE_INDEX).getId())
          .header("token", this.authToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestJson))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.value", is(DEFAULT_VALUE * 2)))
          .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));

      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId())
            .header("token", this.authToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.date", is(expense.getLastDate().toString())));
    }

    // Teste para endpoint DELETE /expense
    @Test
    void deleteExpense_ShouldReturnNoContentAndRemoveExpense() throws Exception {
      final Integer DELETE_INDEX = 1;

      mockMvc.perform(delete("/expense/{id}", savedExpenses.get(DELETE_INDEX).getId())
          .header("token", this.authToken))
          .andExpect(status().isNoContent());

      mockMvc.perform(get("/expense/{id}", savedExpenses.get(DELETE_INDEX).getId())
          .header("token", this.authToken))
          .andExpect(status().isNotFound());

      savedExpenses.remove(savedExpenses.get(DELETE_INDEX));
      for (Expense expense : savedExpenses)
        mockMvc.perform(get("/expense/{id}", expense.getId())
          .header("token", this.authToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.value", is(expense.getValue())))
            .andExpect(jsonPath("$.date", is(expense.getLastDate().toString())));
    }
  }
}
