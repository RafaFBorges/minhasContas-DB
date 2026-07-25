package com.minhascontasdb.inicialization;

import com.minhascontasdb.persistence.CategoryPersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.User;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Order(2)
public class CategoryInitialDataEnsurer implements CommandLineRunner {

  private final CategoryPersistence repository;
  private final UserPersistence userPersistence;

  public CategoryInitialDataEnsurer(CategoryPersistence repository, UserPersistence userPersistence) {
    this.repository = repository;
    this.userPersistence = userPersistence;
  }

  private static final List<String> REQUIRED_CATEGORY_NAMES = Arrays.asList(
      "Alimentação", "Transporte", "Salário", "Moradia");

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    System.out.println("Category Inicialization BEGIN");

    User systemUser = userPersistence.findByUser(UserInitialDataEnsurer.SYSTEM_USERNAME).orElseThrow();

    for (String categoryName : REQUIRED_CATEGORY_NAMES) {
      if (repository.findByNameAndOwner_Id(categoryName, systemUser.getId()).isEmpty()) {
        repository.save(new Category(categoryName, systemUser));
        System.out.println("- Category inserted > '" + categoryName + "'");
      }
    }

    System.out.println("Category Inicialization END");
  }
}