package com.minhascontasdb.controller;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.service.Expense;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

@RestController
@RequestMapping("/expense")
public class ExpensesController {

  @Autowired
  private ExpensePersistence expensePersistence;

  @GetMapping
  public ResponseEntity<ExpenseRequestDTO> getExpense() {
    ExpenseRequestDTO dto = new ExpenseRequestDTO();
    dto.setValue(10.0);
    dto.setDate(Instant.now());
    return ResponseEntity.ok(dto);
  }

  @PostMapping
  public ResponseEntity<Expense> createExpense(@RequestBody ExpenseRequestDTO dto) {
    Expense newExpense = new Expense(dto.getValue(), dto.getDate());

    Expense savedExpense = expensePersistence.save(newExpense);

    return ResponseEntity.ok(savedExpense);
  }
}
