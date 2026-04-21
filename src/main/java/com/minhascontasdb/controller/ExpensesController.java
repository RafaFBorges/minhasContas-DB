package com.minhascontasdb.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.dto.ExpenseResponseDTO;
import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.CategoryPersistence;
import org.springframework.web.bind.annotation.RequestHeader;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.Expense;
import com.minhascontasdb.service.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

  @Autowired
  private CategoryPersistence categoryRepository;

  @Autowired
  private UserPersistence userPersistence;

  @org.springframework.web.bind.annotation.ExceptionHandler(InvalidAccessError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidAccess(InvalidAccessError error) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error.getResponse());
  }

  @ExceptionHandler(InvalidArgumentsError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidArguments(InvalidArgumentsError e) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(e.getResponse());
  }

  @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidDataAccessResourceUsage(
      InvalidDataAccessResourceUsageException e) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new InvalidArgumentsError("A column was not found").getResponse());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception e) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponseDTO("Error=" + e.getMessage()));
  }

  @GetMapping("/user/{id}")
  public ResponseEntity<List<ExpenseResponseDTO>> getExpense(@PathVariable Long id,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    List<Expense> userExpenses = expensePersistence.findByOwnerIdWithDetails(id);
    List<ExpenseResponseDTO> response = userExpenses.stream()
        .map(ExpenseResponseDTO::new)
        .toList();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getExpenseById(@PathVariable Long id, @RequestHeader("token") String token) {
    Login.validateToken(token);

    Expense expense = expensePersistence.findById(id).orElse(null);
    if (expense == null)
      return ResponseEntity.notFound().build();

    return ResponseEntity.ok(new ExpenseResponseDTO(expense));
  }

  @PostMapping
  public ResponseEntity<Expense> createExpense(@RequestBody ExpenseRequestDTO dto,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (!dto.isValidOwner())
      throw new InvalidArgumentsError("Invalid Owner");

    Optional<User> user = userPersistence.findById(dto.getOwner());

    if (user.isEmpty())
      throw new InvalidArgumentsError("Invalid Owner");

    Instant date = Instant.now();
    if (dto.isValidDate())
      date = dto.getDate();

    Expense newExpense = new Expense(dto.getId(), dto.getValue(), date, user.get());

    if (dto.getOwner() != null) {
      User owner = userPersistence.findById(dto.getOwner()).orElse(null);
      newExpense.setOwner(owner);
    }

    if (dto.getCategoryIds() != null) {
      List<Category> categories = categoryRepository.findAllById(dto.getCategoryIds());
      newExpense.setCategories(categories);
    }

    Expense savedExpense = expensePersistence.save(newExpense);

    if (savedExpense.getOwner() != null) {
      Expense reloaded = expensePersistence.findByOwnerIdWithDetails(savedExpense.getOwner().getId())
          .stream()
          .filter(e -> e.getId().equals(savedExpense.getId()))
          .findFirst()
          .orElse(savedExpense);

      return ResponseEntity.ok(reloaded);
    }

    return ResponseEntity.ok(savedExpense);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequestDTO dto,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (!dto.isValidValue())
      throw new InvalidArgumentsError("Invalid Value");

    if (dto.getDate() == null)
      dto.setDate(Instant.now());

    Expense expenseToUpdate = expensePersistence.findById(id).orElse(null);
    if (expenseToUpdate == null)
      return ResponseEntity.notFound().build();

    expenseToUpdate.setValue(dto.getValue(), dto.getDate());

    if (dto.getCategoryIds() != null) {
      List<Category> categories = categoryRepository.findAllById(dto.getCategoryIds());
      expenseToUpdate.setCategories(categories);
    }

    Expense updatedExpense = expensePersistence.save(expenseToUpdate);

    Expense reloaded = expensePersistence.findByOwnerIdWithDetails(updatedExpense.getOwner().getId())
        .stream()
        .filter(e -> e.getId().equals(updatedExpense.getId()))
        .findFirst()
        .orElse(updatedExpense);

    return ResponseEntity.ok(reloaded);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteExpense(@PathVariable Long id, @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (!expensePersistence.existsById(id))
      return ResponseEntity.notFound().build();

    expensePersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
