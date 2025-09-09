package com.minhascontasdb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.service.ExpenseResponseDTO;
import com.minhascontasdb.service.ExpenseRequestDTO;

@RestController
public class Expenses {

  @GetMapping("/expense")
  public ExpenseResponseDTO GetAExpense() {
    ExpenseResponseDTO aExpense = new ExpenseResponseDTO(10.0);

    return aExpense;
  }

  @PostMapping("/expense")
  public ExpenseResponseDTO CreateAExpense(@RequestBody ExpenseRequestDTO expenseRequest) {
    ExpenseResponseDTO aExpense = new ExpenseResponseDTO(expenseRequest.getValue() + 1, expenseRequest.getDate());

    return aExpense;
  }
}
