package com.minhascontasdb.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.minhascontasdb.inicialization.UserInitialDataEnsurer;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.Expense;
import com.minhascontasdb.service.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class AccessControlTests {
  private static final String PASSWORD = "S3nha-forte";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserPersistence userPersistence;

  @Autowired
  private ExpensePersistence expensePersistence;

  @Autowired
  private CategoryPersistence categoryPersistence;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  // Dois usuários independentes por teste: a (quem faz o pedido) e b (dono dos dados alheios)
  private record Account(User user, String token) {
    Long id() {
      return user.getId();
    }
  }

  private Account newAccount(String prefix) throws Exception {
    String username = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    User user = userPersistence.save(new User(prefix, username + "@test.com", passwordEncoder.encode(PASSWORD), username));

    String response = mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("user", username, "password", PASSWORD))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    return new Account(user, JsonPath.read(response, "$.token"));
  }

  private Expense newExpense(Account owner) {
    return expensePersistence.save(new Expense(null, 10.0, Instant.now(), owner.user()));
  }

  private Category newCategory(Account owner, String name) {
    return categoryPersistence.save(new Category(name, owner.user()));
  }

  private Category systemCategory() {
    User system = userPersistence.findByUser(UserInitialDataEnsurer.SYSTEM_USERNAME).orElseThrow();

    return categoryPersistence.findByNameAndOwner_Id("Alimentação", system.getId()).orElseThrow();
  }

  private ResultActions call(MockHttpServletRequestBuilder request, Account as, Object body) throws Exception {
    request = request.header("token", as.token());

    if (body != null)
      request = request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));

    return mockMvc.perform(request);
  }

  // ---------- despesas ----------

  @Test
  void ownExpensesAreListed() throws Exception {
    Account a = newAccount("a");
    newExpense(a);

    call(get("/expense/user/" + a.id()), a, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void listingAnotherUsersExpensesIsNotFound() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");
    newExpense(b);

    call(get("/expense/user/" + b.id()), a, null).andExpect(status().isNotFound());
  }

  @Test
  void anotherUsersExpenseIsNotFoundForReadUpdateAndDelete() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");
    Expense expense = newExpense(b);
    String url = "/expense/" + expense.getId();

    call(get(url), a, null).andExpect(status().isNotFound());
    call(put(url), a, Map.of("value", 99.0)).andExpect(status().isNotFound());
    call(delete(url), a, null).andExpect(status().isNotFound());

    // continua intacta para o dono
    call(get(url), b, null).andExpect(status().isOk());
    assertEquals(10.0, expensePersistence.findById(expense.getId()).orElseThrow().getValue());
  }

  @Test
  void ownerCanReadUpdateAndDeleteTheirExpense() throws Exception {
    Account a = newAccount("a");
    Expense expense = newExpense(a);
    String url = "/expense/" + expense.getId();

    call(put(url), a, Map.of("value", 55.0)).andExpect(status().isOk());
    call(get(url), a, null).andExpect(status().isOk()).andExpect(jsonPath("$.value").value(55.0));
    call(delete(url), a, null).andExpect(status().isNoContent());
    call(get(url), a, null).andExpect(status().isNotFound());
  }

  @Test
  void createExpenseForAnotherOwnerIsNotFoundAndCreatesNothing() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");

    call(post("/expense"), a, Map.of("value", 5.0, "owner", b.id())).andExpect(status().isNotFound());

    assertTrue(expensePersistence.findByOwnerIdWithDetails(b.id()).isEmpty());
  }

  @Test
  void createExpenseForYourselfWorks() throws Exception {
    Account a = newAccount("a");

    call(post("/expense"), a, Map.of("value", 5.0, "owner", a.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.owner").value(a.id().intValue()));
  }

  @Test
  void expenseKeepsOnlyCategoriesTheUserCanSee() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");
    Category others = newCategory(b, "do-b");
    Category mine = newCategory(a, "do-a");
    Category shared = systemCategory();

    call(post("/expense"), a, Map.of("value", 5.0, "owner", a.id(),
        "categoryIds", List.of(others.getId(), mine.getId(), shared.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryIds", hasSize(2)))
        .andExpect(jsonPath("$.categoryIds").value(org.hamcrest.Matchers.containsInAnyOrder(
            mine.getId().intValue(), shared.getId().intValue())));
  }

  // ---------- categorias ----------

  @Test
  void listingAnotherUsersCategoriesIsNotFound() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");

    call(get("/category/user/" + b.id()), a, null).andExpect(status().isNotFound());
    call(get("/category/user/" + a.id()), a, null).andExpect(status().isOk());
  }

  @Test
  void anotherUsersCategoryIsNotFoundForReadUpdateAndDelete() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");
    Category category = newCategory(b, "do-b");
    String url = "/category/" + category.getId();

    call(get(url), a, null).andExpect(status().isNotFound());
    call(put(url), a, Map.of("name", "invadida")).andExpect(status().isNotFound());
    call(delete(url), a, null).andExpect(status().isNotFound());

    assertEquals("do-b", categoryPersistence.findById(category.getId()).orElseThrow().getName());
  }

  @Test
  void systemCategoryIsReadableButNotChangeable() throws Exception {
    Account a = newAccount("a");
    Category shared = systemCategory();
    String url = "/category/" + shared.getId();

    // visível para todos, sem expor dados da conta System
    call(get(url), a, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(shared.getName()))
        .andExpect(content().string(not(containsString("password"))))
        .andExpect(content().string(not(containsString("system@system.com"))));

    call(put(url), a, Map.of("name", "renomeada")).andExpect(status().isForbidden());
    call(delete(url), a, null).andExpect(status().isForbidden());

    assertEquals(shared.getName(), categoryPersistence.findById(shared.getId()).orElseThrow().getName());
  }

  @Test
  void categoryCanOnlyBeCreatedForYourself() throws Exception {
    Account a = newAccount("a");
    Account b = newAccount("b");

    call(post("/category"), a, Map.of("name", "minha", "owner", a.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.owner").value(a.id().intValue()))
        .andExpect(content().string(not(containsString("password"))));

    call(post("/category"), a, Map.of("name", "invasora", "owner", b.id())).andExpect(status().isNotFound());
    assertTrue(categoryPersistence.findCategoryDTOsByOwnerId(b.id()).isEmpty());
  }

  @Test
  void ownerCanUpdateAndDeleteTheirCategory() throws Exception {
    Account a = newAccount("a");
    Category category = newCategory(a, "antes");
    String url = "/category/" + category.getId();

    call(put(url), a, Map.of("name", "depois"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("depois"));
    call(delete(url), a, null).andExpect(status().isNoContent());
    call(get(url), a, null).andExpect(status().isNotFound());
  }

  // ---------- sem token ----------

  @Test
  void missingOrInvalidTokenIsUnauthorized() throws Exception {
    mockMvc.perform(get("/expense/user/1")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/category/user/1").header("token", "invalido")).andExpect(status().isUnauthorized());
  }
}
