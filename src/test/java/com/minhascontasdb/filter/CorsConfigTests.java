package com.minhascontasdb.filter;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CorsConfigTests {
  @Test
  void emptyOrMissingValueMeansNoOrigins() {
    assertTrue(CorsConfig.parseOrigins("").isEmpty());
    assertTrue(CorsConfig.parseOrigins("   ").isEmpty());
    assertTrue(CorsConfig.parseOrigins(null).isEmpty());
    assertTrue(CorsConfig.parseOrigins(" , ,").isEmpty());
  }

  @Test
  void listIsSplitAndTrimmed() {
    assertEquals(List.of("https://a.com", "http://localhost:3000"),
        CorsConfig.parseOrigins(" https://a.com ,http://localhost:3000 ,"));
  }

  // Formas que o navegador nunca enviaria como Origin: subiriam "funcionando" e responderiam 403 a tudo
  @Test
  void malformedOriginsAreRefusedAtStartup() {
    for (String invalid : List.of(
        "localhost:3000",            // sem esquema
        "https://a.com/",            // barra final
        "https://a.com/app",         // caminho
        "https://a.com?x=1",         // query
        "\"https://a.com\"",         // aspas coladas do .env
        "ftp://a.com",               // esquema que não é http(s)
        "https://",                  // sem host
        "a.com"))
      assertThrows(IllegalStateException.class, () -> CorsConfig.parseOrigins(invalid), invalid);

    // Uma inválida no meio da lista derruba a subida, em vez de ser ignorada
    assertThrows(IllegalStateException.class, () -> CorsConfig.parseOrigins("https://a.com, localhost:3000"));
  }

  @Test
  void validOriginsAreAccepted() {
    assertEquals(List.of("https://app.exemplo.com", "http://localhost:3000", "https://a.com:8443", "http://127.0.0.1:5173"),
        CorsConfig.parseOrigins("https://app.exemplo.com,http://localhost:3000, https://a.com:8443 ,http://127.0.0.1:5173"));
  }

  @Test
  void wildcardIsRefusedAtStartup() {
    assertThrows(IllegalStateException.class, () -> CorsConfig.parseOrigins("*"));
    assertThrows(IllegalStateException.class, () -> CorsConfig.parseOrigins("https://a.com, *"));
    assertThrows(IllegalStateException.class, () -> CorsConfig.parseOrigins("https://*.exemplo.com"));
  }
}
