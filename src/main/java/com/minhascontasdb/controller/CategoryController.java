package com.minhascontasdb.controller;

import java.util.List;
import java.util.Optional;

import com.minhascontasdb.dto.CategoryRequestDTO;
import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
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

  @Autowired
  private CategoryPersistence categoryPersistence;

  @Autowired
  private UserPersistence userPersistence;

  @GetMapping("/user/{id}")
  public ResponseEntity<?> getCategory(@PathVariable Long id) {
    try {
      User owner = userPersistence.findById(id).orElse(null);
      if (owner == null)
        return ResponseEntity.notFound().build();

      List<Category> allCategories = categoryPersistence.findByOwner_id(id);

      return ResponseEntity.ok(allCategories);
    } catch (InvalidDataAccessResourceUsageException e) {
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new InvalidArgumentsError("A column was not found").getResponse());
    } catch (Exception e) {
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ErrorResponseDTO("Error=" + e.getMessage()));
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
    Optional<Category> category = categoryPersistence.findById(id);

    if (category.isPresent())
      return ResponseEntity.ok(category.get());
    else
      return ResponseEntity.notFound().build();
  }

  @PostMapping
  public ResponseEntity<?> createCategory(@RequestBody CategoryRequestDTO dto) {
    try {
      User owner = userPersistence.findById(dto.getOwner())
          .orElseThrow(() -> new InvalidArgumentsError("Owner user not found"));

      Category newCategory = new Category(dto.getName(), owner);

      Category savedCategory = categoryPersistence.save(newCategory);
      return ResponseEntity.ok(savedCategory);
    } catch (InvalidArgumentsError error) {
      return ResponseEntity.badRequest().body(error.getResponse());
    } catch (InvalidDataAccessResourceUsageException error) {
      return ResponseEntity.badRequest().body(error);
    } catch (DataIntegrityViolationException error) {
      return ResponseEntity.badRequest().body(new InvalidArgumentsError(error.getMessage()).getResponse());
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequestDTO dto) {
    try {
      if (dto.getName() == null)
        return ResponseEntity.badRequest().body(new InvalidArgumentsError().getResponse());

      Optional<Category> existingCategory = categoryPersistence.findById(id);

      if (!existingCategory.isPresent())
        return ResponseEntity.notFound().build();

      Category categoryToUpdate = existingCategory.get();
      categoryToUpdate.setName(dto.getName());

      Category updatedCategory = categoryPersistence.save(categoryToUpdate);
      return ResponseEntity.ok(updatedCategory);
    } catch (InvalidArgumentsError error) {
      return ResponseEntity.badRequest().body(error.getResponse());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
    if (!categoryPersistence.existsById(id))
      return ResponseEntity.notFound().build();

    categoryPersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
