package com.minhascontasdb.service;

// Hierarquia: SYSTEM > ADMIN > SUPPORT > USER (cada papel pode o que os de baixo podem).
// SYSTEM é só a conta dona dos dados compartilhados: nunca faz login nem recebe token.
public enum Role {
  USER,
  SUPPORT,
  ADMIN,
  SYSTEM;

  public String authority() {
    return "ROLE_" + name();
  }

  // Formato do RoleHierarchy do Spring Security
  public static String hierarchy() {
    return SYSTEM.authority() + " > " + ADMIN.authority() + "\n"
        + ADMIN.authority() + " > " + SUPPORT.authority() + "\n"
        + SUPPORT.authority() + " > " + USER.authority();
  }
}
