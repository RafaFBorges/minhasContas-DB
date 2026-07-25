package com.minhascontasdb.controller;

import java.util.List;
import java.util.Optional;

import com.minhascontasdb.dto.CategoryRequestDTO;
import com.minhascontasdb.dto.CategoryResponseDTO;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.UserPersistence;
import org.springframework.web.bind.annotation.RequestHeader;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.User;

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
@RequestMapping("/category")
@CrossOrigin(origins = "*")
public class CategoryController {

  private final CategoryPersistence categoryPersistence;
  private final UserPersistence userPersistence;

  // Injeção de dependências via Construtor
  public CategoryController(CategoryPersistence categoryPersistence, UserPersistence userPersistence) {
    this.categoryPersistence = categoryPersistence;
    this.userPersistence = userPersistence;
  }

  @GetMapping("/user/{id}")
  public ResponseEntity<List<CategoryResponseDTO>> getCategory(@PathVariable Long id,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (!userPersistence.existsById(id))
      return ResponseEntity.notFound().build();

    List<CategoryResponseDTO> allCategories = categoryPersistence.findCategoryDTOsByOwnerId(id);

    return ResponseEntity.ok(allCategories);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Category> getCategoryById(@PathVariable Long id, @RequestHeader("token") String token) {
    Login.validateToken(token);

    return categoryPersistence.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<Category> createCategory(@RequestBody CategoryRequestDTO dto,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    User owner = userPersistence.findById(dto.getOwner())
        .orElseThrow(() -> new InvalidArgumentsError("Owner user not found"));

    Category newCategory = new Category(dto.getName(), owner);

    Category savedCategory = categoryPersistence.save(newCategory);
    return ResponseEntity.ok(savedCategory);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody CategoryRequestDTO dto,
      @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (dto.getName() == null)
      throw new InvalidArgumentsError("Empty name");

    Optional<Category> existingCategory = categoryPersistence.findById(id);

    if (!existingCategory.isPresent())
      return ResponseEntity.notFound().build();

    Category categoryToUpdate = existingCategory.get();
    categoryToUpdate.setName(dto.getName());

    Category updatedCategory = categoryPersistence.save(categoryToUpdate);
    return ResponseEntity.ok(updatedCategory);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id, @RequestHeader("token") String token) {
    Login.validateToken(token);

    if (!categoryPersistence.existsById(id))
      return ResponseEntity.notFound().build();

    categoryPersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
