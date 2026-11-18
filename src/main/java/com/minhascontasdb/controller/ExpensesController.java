package com.minhascontasdb.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.minhascontasdb.dto.ExpenseRequestDTO;
import com.minhascontasdb.dto.ExpenseResponseDTO;
import com.minhascontasdb.dto.AuthUser;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.dto.Errors.NotFoundError;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.ExpensePersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.AccessPolicy;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.Expense;
import com.minhascontasdb.service.User;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  // Despesa de outro usuário não existe para quem pede
  private Expense findOwnedExpense(Long id, AuthUser session) {
    return expensePersistence.findById(id)
        .filter(expense -> AccessPolicy.isOwnedBy(expense.getOwner(), session))
        .orElseThrow(() -> new NotFoundError("Expense not found"));
  }

  // Categoria de outro usuário não existe para quem pede, igual a um id inexistente: qualquer id
  // que não seja visível derruba o pedido inteiro, sem alterar nada
  private List<Category> findReadableCategories(List<Long> ids, AuthUser session) {
    Set<Long> requested = new HashSet<>(ids);

    List<Category> readable = categoryRepository.findAllById(requested).stream()
        .filter(category -> AccessPolicy.canRead(category, session))
        .collect(Collectors.toCollection(ArrayList::new));

    if (readable.size() != requested.size())
      throw new NotFoundError("Category not found");

    return readable;
  }

  @GetMapping("/user/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<List<ExpenseResponseDTO>> getExpense(@PathVariable Long id,
      @AuthenticationPrincipal AuthUser session) {
    if (!session.id().equals(id))
      throw new NotFoundError("User not found");

    List<Expense> userExpenses = expensePersistence.findByOwnerIdWithDetails(id);
    List<ExpenseResponseDTO> response = userExpenses.stream()
        .map(ExpenseResponseDTO::new)
        .toList();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<ExpenseResponseDTO> getExpenseById(@PathVariable Long id,
      @AuthenticationPrincipal AuthUser session) {
    return ResponseEntity.ok(new ExpenseResponseDTO(findOwnedExpense(id, session)));
  }

  @PostMapping
  public ResponseEntity<ExpenseResponseDTO> createExpense(@RequestBody ExpenseRequestDTO dto,
      @AuthenticationPrincipal AuthUser session) {
    if (!dto.isValidOwner())
      throw new InvalidArgumentsError("Invalid Owner");

    if (!session.id().equals(dto.getOwner()))
      throw new NotFoundError("Owner not found");

    User owner = userPersistence.findById(session.id())
        .orElseThrow(() -> new InvalidArgumentsError("Invalid Owner"));

    Instant date = Instant.now();
    if (dto.isValidDate())
      date = dto.getDate();

    // O id nunca vem do cliente: save() com id existente faria merge e sobrescreveria a despesa de outro usuário
    Expense newExpense = new Expense(null, dto.getValue(), date, owner);

    if (dto.getCategoryIds() != null)
      newExpense.setCategories(findReadableCategories(dto.getCategoryIds(), session));

    Expense savedExpense = expensePersistence.save(newExpense);

    Expense reloaded = expensePersistence.findByOwnerIdWithDetails(owner.getId())
        .stream()
        .filter(e -> e.getId().equals(savedExpense.getId()))
        .findFirst()
        .orElse(savedExpense);

    return ResponseEntity.ok(new ExpenseResponseDTO(reloaded));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ExpenseResponseDTO> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequestDTO dto,
      @AuthenticationPrincipal AuthUser session) {
    if (!dto.isValidValue())
      throw new InvalidArgumentsError("Invalid Value");

    if (dto.getDate() == null)
      dto.setDate(Instant.now());

    Expense expenseToUpdate = findOwnedExpense(id, session);

    List<Category> categories = dto.getCategoryIds() != null
        ? findReadableCategories(dto.getCategoryIds(), session)
        : null;

    expenseToUpdate.setValue(dto.getValue(), dto.getDate());

    if (categories != null)
      expenseToUpdate.setCategories(categories);

    Expense updatedExpense = expensePersistence.save(expenseToUpdate);

    Expense reloaded = expensePersistence.findByOwnerIdWithDetails(updatedExpense.getOwner().getId())
        .stream()
        .filter(e -> e.getId().equals(updatedExpense.getId()))
        .findFirst()
        .orElse(updatedExpense);

    return ResponseEntity.ok(new ExpenseResponseDTO(reloaded));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteExpense(@PathVariable Long id, @AuthenticationPrincipal AuthUser session) {
    findOwnedExpense(id, session);

    expensePersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
