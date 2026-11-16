package com.minhascontasdb.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordPolicyTests {
  @Test
  void acceptsPasswordsWithEveryRequiredKind() {
    assertTrue(PasswordPolicy.isStrong("S3nha-forte"));
    assertTrue(PasswordPolicy.isStrong("Abcdef1!"));
    assertTrue(PasswordPolicy.isStrong("Açaí123!x"));
  }

  @Test
  void rejectsPasswordsMissingAnyRequirement() {
    assertFalse(PasswordPolicy.isStrong("Ab1!"));          // curta
    assertFalse(PasswordPolicy.isStrong("Abcde1!"));       // 7 caracteres
    assertFalse(PasswordPolicy.isStrong("abcdef1!"));      // sem maiúscula
    assertFalse(PasswordPolicy.isStrong("ABCDEF1!"));      // sem minúscula
    assertFalse(PasswordPolicy.isStrong("Abcdefg!"));      // sem número
    assertFalse(PasswordPolicy.isStrong("Abcdefg1"));      // sem especial
    assertFalse(PasswordPolicy.isStrong("        "));
    assertFalse(PasswordPolicy.isStrong(""));
    assertFalse(PasswordPolicy.isStrong(null));
  }
}
