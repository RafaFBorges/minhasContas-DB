package com.minhascontasdb.service;

import com.minhascontasdb.dto.AuthUser;

// Regras de quem pode ver ou alterar o quê.
// Para quem pede, o que é de outro usuário não existe (404). Só os dados da conta System
// são visíveis para todos, mas não podem ser alterados (403). Alterá-las por um Admin ainda não foi implementado.
public final class AccessPolicy {
  private AccessPolicy() {
  }

  public static boolean isOwnedBy(User owner, AuthUser session) {
    return owner != null && session.id().equals(owner.getId());
  }

  public static boolean isSystemOwned(Category category) {
    User owner = category.getOwner();

    return owner != null && owner.isSystem();
  }

  public static boolean canRead(Category category, AuthUser session) {
    return isOwnedBy(category.getOwner(), session) || isSystemOwned(category);
  }
}
