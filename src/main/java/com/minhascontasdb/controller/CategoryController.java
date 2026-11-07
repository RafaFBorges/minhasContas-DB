package com.minhascontasdb.controller;

import java.util.List;

import com.minhascontasdb.dto.CategoryRequestDTO;
import com.minhascontasdb.dto.CategoryResponseDTO;
import com.minhascontasdb.dto.UserSession;
import com.minhascontasdb.dto.Errors.ForbiddenError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.dto.Errors.NotFoundError;
import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.UserPersistence;
import org.springframework.web.bind.annotation.RequestHeader;
import com.minhascontasdb.service.AccessPolicy;
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

  // Categoria de outro usuário não existe para quem pede. As da conta System são visíveis a todos
  private Category findReadableCategory(Long id, UserSession session) {
    return categoryPersistence.findById(id)
        .filter(category -> AccessPolicy.canRead(category, session))
        .orElseThrow(() -> new NotFoundError("Category not found"));
  }

  // Só quem é dono altera. As categorias da conta System são visíveis, mas só um admin poderá alterá-las
  private Category findCategoryToChange(Long id, UserSession session) {
    Category category = findReadableCategory(id, session);

    if (!AccessPolicy.isOwnedBy(category.getOwner(), session))
      throw new ForbiddenError("Shared category");

    return category;
  }

  @GetMapping("/user/{id}")
  public ResponseEntity<List<CategoryResponseDTO>> getCategory(@PathVariable Long id,
      @RequestHeader("token") String token) {
    UserSession session = Login.validateToken(token);

    if (!session.id().equals(id))
      throw new NotFoundError("User not found");

    List<CategoryResponseDTO> allCategories = categoryPersistence.findCategoryDTOsByOwnerId(id);

    return ResponseEntity.ok(allCategories);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id,
      @RequestHeader("token") String token) {
    UserSession session = Login.validateToken(token);

    return ResponseEntity.ok(new CategoryResponseDTO(findReadableCategory(id, session)));
  }

  @PostMapping
  public ResponseEntity<CategoryResponseDTO> createCategory(@RequestBody CategoryRequestDTO dto,
      @RequestHeader("token") String token) {
    UserSession session = Login.validateToken(token);

    if (dto.getOwner() == null)
      throw new InvalidArgumentsError("Owner user not found");

    if (!session.id().equals(dto.getOwner()))
      throw new NotFoundError("Owner not found");

    User owner = userPersistence.findById(session.id())
        .orElseThrow(() -> new InvalidArgumentsError("Owner user not found"));

    Category newCategory = new Category(dto.getName(), owner);

    return ResponseEntity.ok(new CategoryResponseDTO(categoryPersistence.save(newCategory)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long id, @RequestBody CategoryRequestDTO dto,
      @RequestHeader("token") String token) {
    UserSession session = Login.validateToken(token);

    if (dto.getName() == null)
      throw new InvalidArgumentsError("Empty name");

    Category categoryToUpdate = findCategoryToChange(id, session);
    categoryToUpdate.setName(dto.getName());

    return ResponseEntity.ok(new CategoryResponseDTO(categoryPersistence.save(categoryToUpdate)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id, @RequestHeader("token") String token) {
    UserSession session = Login.validateToken(token);

    findCategoryToChange(id, session);

    categoryPersistence.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
