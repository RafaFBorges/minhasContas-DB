package com.minhascontasdb.service;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.minhascontasdb.persistence.RefreshTokenPersistence;
import com.minhascontasdb.persistence.UserPersistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RefreshTokenServiceConfigTests {
  private static RefreshTokenService build(Duration ttl, Duration chain, Duration grace, int sessions) {
    return new RefreshTokenService(mock(RefreshTokenPersistence.class), mock(UserPersistence.class),
        "0123456789abcdef0123456789abcdef", ttl, chain, grace, sessions, Clock.systemUTC());
  }

  @Test
  void validConfigurationIsAccepted() {
    assertDoesNotThrow(() -> build(Duration.ofDays(7), Duration.ofDays(30), Duration.ofSeconds(10), 10));
    // grace 0 desliga a tolerância
    assertDoesNotThrow(() -> build(Duration.ofDays(7), Duration.ofDays(30), Duration.ZERO, 10));
  }

  @Test
  void missingOrWeakSecretFailsTheStartup() {
    for (String secret : new String[] { null, "curto" })
      assertThrows(IllegalStateException.class, () -> new RefreshTokenService(mock(RefreshTokenPersistence.class),
          mock(UserPersistence.class), secret, Duration.ofDays(7), Duration.ofDays(30), Duration.ZERO, 10, Clock.systemUTC()));
  }

  @Test
  void zeroOrNegativeSettingsFailTheStartup() {
    assertThrows(IllegalStateException.class, () -> build(Duration.ZERO, Duration.ofDays(30), Duration.ZERO, 10));
    assertThrows(IllegalStateException.class, () -> build(Duration.ofDays(-1), Duration.ofDays(30), Duration.ZERO, 10));
    assertThrows(IllegalStateException.class, () -> build(Duration.ofDays(7), Duration.ZERO, Duration.ZERO, 10));
    assertThrows(IllegalStateException.class, () -> build(Duration.ofDays(7), Duration.ofDays(-30), Duration.ZERO, 10));
    assertThrows(IllegalStateException.class, () -> build(Duration.ofDays(7), Duration.ofDays(30), Duration.ofSeconds(-1), 10));
    assertThrows(IllegalStateException.class, () -> build(Duration.ofDays(7), Duration.ofDays(30), Duration.ZERO, 0));
  }
}
