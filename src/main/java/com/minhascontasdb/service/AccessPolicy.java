package com.minhascontasdb.service;

import com.minhascontasdb.dto.UserSession;
import com.minhascontasdb.inicialization.UserInitialDataEnsurer;

// Regras de quem pode ver ou alterar o quê.
// Para quem pede, o que é de outro usuário não existe (404). Só os dados da conta System
// são visíveis para todos, mas não podem ser alterados (403) até existirem papéis de admin.
public final class AccessPolicy {
  private AccessPolicy() {
  }

  public static boolean isOwnedBy(User owner, UserSession session) {
    return owner != null && session.id().equals(owner.getId());
  }

  public static boolean isSystemOwned(Category category) {
    User owner = category.getOwner();

    return owner != null && UserInitialDataEnsurer.SYSTEM_USERNAME.equals(owner.getUser());
  }

  public static boolean canRead(Category category, UserSession session) {
    return isOwnedBy(category.getOwner(), session) || isSystemOwned(category);
  }
}
