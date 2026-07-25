package com.minhascontasdb.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.dto.ExpenseResponseDTO;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.dto.Errors.NotFoundError;
import com.minhascontasdb.persistence.CategoryPersistence;
import org.springframework.web.bind.annotation.RequestHeader;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.Expense;
import com.minhascontasdb.service.User;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

  private final ExpensePersistence expensePersistence;
  private final CategoryPersistence categoryRepository;
  private final UserPersistence userPersistence;

  // Injeção de dependências via Construtor
  public ExpensesController(
      ExpensePersistence expensePersistence,
      CategoryPersistence categoryRepository,
      UserPersistence userPersistence) {
    this.expensePersistence = expensePersistence;
    this.categoryRepository = categoryRepository;
    this.userPersistence = userPersistence;
  }

  @GetMapping("/user/{id}")
  @Transactional(readOnly = true)
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
  @Transactional(readOnly = true)
  public ResponseEntity<ExpenseResponseDTO> getExpenseById(@PathVariable Long id,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    Expense expense = expensePersistence.findById(id).orElse(null);
    if (expense == null)
      throw new NotFoundError("Expense not found");

    return ResponseEntity.ok(new ExpenseResponseDTO(expense));
  }

  @PostMapping
  public ResponseEntity<ExpenseResponseDTO> createExpense(@RequestBody ExpenseRequestDTO dto,
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

      return ResponseEntity.ok(new ExpenseResponseDTO(reloaded));
    }

    return ResponseEntity.ok(new ExpenseResponseDTO(savedExpense));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ExpenseResponseDTO> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequestDTO dto,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (!dto.isValidValue())
      throw new InvalidArgumentsError("Invalid Value");

    if (dto.getDate() == null)
      dto.setDate(Instant.now());

    Expense expenseToUpdate = expensePersistence.findById(id).orElse(null);
    if (expenseToUpdate == null)
      throw new NotFoundError("Expense not found");

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

    return ResponseEntity.ok(new ExpenseResponseDTO(reloaded));
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
