package com.minhascontasdb.inicialization;

import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.Role;
import com.minhascontasdb.service.User;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@Order(1)
public class UserInitialDataEnsurer implements CommandLineRunner {

  private final UserPersistence userPersistence;
  private final PasswordEncoder passwordEncoder;
  public static final String SYSTEM_USERNAME = "System";

  // O nome da conta de serviço é reservado em qualquer combinação de maiúsculas e minúsculas (cadastro e Admin inicial)
  public static boolean isReserved(String username) {
    return SYSTEM_USERNAME.equalsIgnoreCase(username);
  }

  public UserInitialDataEnsurer(UserPersistence userPersistence, PasswordEncoder passwordEncoder) {
    this.userPersistence = userPersistence;
    this.passwordEncoder = passwordEncoder;
  }

  // Senha que ninguém conhece: a conta System é só dona de dados e não faz login
  private String randomHash() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);

    return passwordEncoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
  }

  @Override
  @Transactional
  public void run(String... args) {
    System.out.println("User Inicialization BEGIN");
    User systemUser = userPersistence.findByUser(SYSTEM_USERNAME)
        .orElseGet(() -> new User("System", "system@system.com", "", SYSTEM_USERNAME));
    boolean isNew = systemUser.getId() == null;

    // A cada subida a senha é trocada por outra aleatória, inclusive em bancos antigos onde era "system"
    systemUser.setRole(Role.SYSTEM);
    systemUser.setPassword(randomHash());
    userPersistence.save(systemUser);
    System.out.println(isNew ? "System user created" : "System user password renewed");

    System.out.println("User Inicialization END");
  }
}
