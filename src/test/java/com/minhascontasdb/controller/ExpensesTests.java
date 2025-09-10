package com.minhascontasdb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhascontasdb.dto.ExpenseRequestDTO;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

import java.time.Instant;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class ExpensesTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void contextLoads() {
    // Teste básico para verificar se o contexto do Spring carrega corretamente.
  }

  @Test
  @Disabled
  void getExpense_() throws Exception {
    mockMvc.perform(get("/expense"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.value", is(10.0)))
        .andExpect(jsonPath("$.date").exists());
  }

  @Test
  void createExpense_ShouldReturnCreatedExpense() throws Exception {
    final Double DEFAULT_vALUE = 50.0;

    ExpenseRequestDTO requestDTO = new ExpenseRequestDTO();
    requestDTO.setValue(DEFAULT_vALUE);
    requestDTO.setDate(Instant.now());

    String requestJson = objectMapper.writeValueAsString(requestDTO);

    mockMvc.perform(post("/expense")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.value", is(DEFAULT_vALUE)))
        .andExpect(jsonPath("$.date", is(requestDTO.getDate().toString())));
  }
}
