package com.minhascontasdb.service;

import java.util.regex.Pattern;

// Regra mínima de senha no servidor. Espelha validateStrongPassword do front (componentes/src/utils/validations.ts):
// mínimo de 8 caracteres, com minúscula, maiúscula, número e caractere especial.
// O front só ajuda a pessoa a digitar: é aqui que a regra vale de verdade, pois a API pode ser chamada sem o front.
// Qualquer endpoint que defina ou troque senha (cadastro, troca, reset) precisa chamar isStrong
public final class PasswordPolicy {
  public static final int MIN_LENGTH = 8;

  private static final Pattern STRONG = Pattern.compile(
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{" + MIN_LENGTH + ",}$");

  private PasswordPolicy() {
  }

  // Chamar só depois de limitar o tamanho da senha (Register.MAX_PASSWORD_BYTES): a regex não deve ver entradas enormes
  public static boolean isStrong(String password) {
    return password != null && STRONG.matcher(password).matches();
  }
}
