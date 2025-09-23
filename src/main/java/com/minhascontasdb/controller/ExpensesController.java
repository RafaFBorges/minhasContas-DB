package com.minhascontasdb.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.service.Expense;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expense")
@CrossOrigin(origins = "*")
public class ExpensesController {

  @Autowired
  private ExpensePersistence expensePersistence;

  @GetMapping
  public ResponseEntity<List<Expense>> getExpense() {

    List<Expense> allExpenses = expensePersistence.findAll();
    return ResponseEntity.ok(allExpenses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Expense> getExpenseById(@PathVariable Long id) {
    Optional<Expense> expense = expensePersistence.findById(id);

    if (expense.isPresent())
      return ResponseEntity.ok(expense.get());
    else
      return ResponseEntity.notFound().build();
  }

  @PostMapping
  public ResponseEntity<Expense> createExpense(@RequestBody ExpenseRequestDTO dto) {
    Expense newExpense = new Expense(dto.getValue(), dto.getDate());

    Expense savedExpense = expensePersistence.save(newExpense);

    return ResponseEntity.ok(savedExpense);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequestDTO dto) {
    if (!dto.isValidValue())
      return ResponseEntity.badRequest().body(new InvalidArgumentsError());

    if (dto.getDate() == null)
      dto.setDate(Instant.now());

    Optional<Expense> existingExpense = expensePersistence.findById(id);

    if (!existingExpense.isPresent())
      return ResponseEntity.notFound().build();

    Expense expenseToUpdate = existingExpense.get();
    expenseToUpdate.setValue(dto.getValue(), dto.getDate());

    Expense updatedExpense = expensePersistence.save(expenseToUpdate);
    return ResponseEntity.ok(updatedExpense);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
    if (!expensePersistence.existsById(id))
      return ResponseEntity.notFound().build();

    expensePersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
