package com.minhascontasdb.inicialization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.PasswordPolicy;
import com.minhascontasdb.service.Role;
import com.minhascontasdb.service.User;

// Cria o primeiro Admin a partir das variáveis ADMIN_USER, ADMIN_EMAIL e ADMIN_PASSWORD, só se ainda não existir nenhum.
// Depois de criado, o Admin vive no banco: trocar a variável não altera nem recria a conta.
// Configuração inválida (variáveis incompletas, senha fraca, nome reservado) derruba a subida. Já usuário ou email em uso por outra
// conta só gera um aviso e o bootstrap é ignorado: é o que sobra quando o Admin foi rebaixado e as variáveis ficaram no ambiente,
// e isso não pode impedir a API de subir
@Component
@Order(3)
public class AdminInitialDataEnsurer implements CommandLineRunner {
  private final UserPersistence userPersistence;
  private final PasswordEncoder passwordEncoder;
  private final String username;
  private final String email;
  private final String password;

  public AdminInitialDataEnsurer(UserPersistence userPersistence, PasswordEncoder passwordEncoder,
      @Value("${admin.user:}") String username,
      @Value("${admin.email:}") String email,
      @Value("${admin.password:}") String password) {
    this.userPersistence = userPersistence;
    this.passwordEncoder = passwordEncoder;
    this.username = username == null ? "" : username.trim();
    this.email = email == null ? "" : email.trim();
    this.password = password == null ? "" : password;
  }

  // Sem @Transactional de propósito: o save tem a própria transação, e o erro de chave duplicada (duas instâncias subindo
  // juntas) precisa ser tratado aqui, não no commit de uma transação externa
  @Override
  public void run(String... args) {
    System.out.println("Admin Inicialization BEGIN");

    if (userPersistence.existsByRole(Role.ADMIN)) {
      System.out.println("Admin already exists");
      if (!(username.isEmpty() && email.isEmpty() && password.isEmpty()))
        System.out.println("WARNING: ADMIN_USER/ADMIN_EMAIL/ADMIN_PASSWORD are still set but only matter on a first boot; remove them from the environment");
    } else if (username.isEmpty() && email.isEmpty() && password.isEmpty()) {
      System.out.println("No Admin configured (set ADMIN_USER, ADMIN_EMAIL and ADMIN_PASSWORD to create one)");
    } else {
      createAdmin();
    }

    System.out.println("Admin Inicialization END");
  }

  private void createAdmin() {
    if (username.isEmpty() || email.isEmpty() || password.isBlank())
      throw new IllegalStateException("ADMIN_USER, ADMIN_EMAIL and ADMIN_PASSWORD must all be set");

    // As mesmas regras do cadastro. A senha nunca vai para a mensagem de erro
    if (PasswordPolicy.isTooLong(password) || !PasswordPolicy.isStrong(password))
      throw new IllegalStateException("ADMIN_PASSWORD does not meet the password policy");
    if (UserInitialDataEnsurer.isReserved(username))
      throw new IllegalStateException("ADMIN_USER is reserved");
    if (userPersistence.existsByUser(username) || userPersistence.existsByEmail(email)) {
      System.out.println("WARNING: no Admin exists, but ADMIN_USER or ADMIN_EMAIL belongs to another account; Admin not created. "
          + "Promote that account in the database or change the variables (and remove them once an Admin exists)");
      return;
    }

    User admin = new User("Admin", email, passwordEncoder.encode(password), username);
    admin.setRole(Role.ADMIN);

    try {
      userPersistence.save(admin);
      System.out.println("Admin created");
    } catch (DataIntegrityViolationException e) {
      // Outra instância criou o Admin entre a checagem e o save: nesse caso está tudo certo
      if (!userPersistence.existsByRole(Role.ADMIN))
        throw e;

      System.out.println("Admin already exists (created by another instance)");
    }
  }
}
