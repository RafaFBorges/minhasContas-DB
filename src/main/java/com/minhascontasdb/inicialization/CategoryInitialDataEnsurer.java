package com.minhascontasdb.inicialization;

import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.service.Category;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryInitialDataEnsurer implements CommandLineRunner {

  private final CategoryPersistence repository;

  public CategoryInitialDataEnsurer(CategoryPersistence repository) {
    this.repository = repository;
  }

  private static final List<Category> REQUIRED_CATEGORIES = Arrays.asList(
      new Category("Alimentação", (long) 0),
      new Category("Transporte", (long) 0),
      new Category("Salário", (long) 0),
      new Category("Moradia", (long) 0));

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    System.out.println("Category Inicialization BEGIN");
    ensureRequiredCategoriesExist();
  }

  private void ensureRequiredCategoriesExist() {
    for (Category requiredCategory : REQUIRED_CATEGORIES) {

      Optional<Category> existingCategory = repository.findByNameAndOwner(requiredCategory.getName(),
          requiredCategory.getOwner());

      if (existingCategory.isEmpty()) {
        repository.save(requiredCategory);
        System.out.println("- Category inserted > '" + requiredCategory.getName() + "'");
      }
    }

    System.out.println("Category Inicialization END");
  }
}