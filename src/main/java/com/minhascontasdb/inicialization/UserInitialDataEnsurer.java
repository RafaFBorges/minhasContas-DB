package com.minhascontasdb.inicialization;

import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class UserInitialDataEnsurer implements CommandLineRunner {

  private final UserPersistence userPersistence;
  public static final String SYSTEM_USERNAME = "System";

  public UserInitialDataEnsurer(UserPersistence userPersistence) {
    this.userPersistence = userPersistence;
  }

  @Override
  @Transactional
  public void run(String... args) {
    System.out.println("User Inicialization BEGIN");
    if (!userPersistence.existsByUser(SYSTEM_USERNAME)) {
      User systemUser = new User("System", "system@system.com", "system", SYSTEM_USERNAME);
      userPersistence.save(systemUser);
      System.out.println("System user created");
    }

    System.out.println("User Inicialization END");
  }
}
