package com.minhascontasdb.controller;

import java.util.List;
import java.util.Optional;

import com.minhascontasdb.dto.CategoryRequestDTO;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.service.Category;

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
@RequestMapping("/category")
@CrossOrigin(origins = "*")
public class CategoryController {

  @Autowired
  private CategoryPersistence categoryPersistence;

  @GetMapping
  public ResponseEntity<List<Category>> getCategory() {
    List<Category> allCategories = categoryPersistence.findAll();
    return ResponseEntity.ok(allCategories);
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
  public ResponseEntity<Category> createCategory(@RequestBody CategoryRequestDTO dto) {
    Category newCategory = new Category(dto.getName());

    Category savedCategory = categoryPersistence.save(newCategory);

    return ResponseEntity.ok(savedCategory);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequestDTO dto) {
    if (dto.getName() == null)
      return ResponseEntity.badRequest().body(new InvalidArgumentsError());

    Optional<Category> existingCategory = categoryPersistence.findById(id);

    if (!existingCategory.isPresent())
      return ResponseEntity.notFound().build();

    Category categoryToUpdate = existingCategory.get();
    categoryToUpdate.setName(dto.getName());

    Category updatedCategory = categoryPersistence.save(categoryToUpdate);
    return ResponseEntity.ok(updatedCategory);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
    if (!categoryPersistence.existsById(id))
      return ResponseEntity.notFound().build();

    categoryPersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
