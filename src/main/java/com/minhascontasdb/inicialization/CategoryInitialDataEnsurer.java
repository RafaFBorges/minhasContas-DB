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
  @Transactional // Garante que a operação ocorra em uma transação
  public void run(String... args) throws Exception {
    System.out.println("✅ Verificando dados iniciais no banco de dados...");
    ensureRequiredCategoriesExist();
  }

  private void ensureRequiredCategoriesExist() {
    for (Category requiredCategory : REQUIRED_CATEGORIES) {

      // 1. Verifica se a categoria já existe (ex: pelo nome)
      Optional<Category> existingCategory = repository.findByName(requiredCategory.getName());

      if (existingCategory.isEmpty()) {

        // 2. Se não existe, salva o novo objeto
        repository.save(requiredCategory);
        System.out.println("✨ Inserido: Categoria '" + requiredCategory.getName() + "'");
      }
    }
  }
}