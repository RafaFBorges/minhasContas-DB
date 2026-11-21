package com.minhascontasdb.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTests {
  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  private static User user() {
    User user = new User("Nome", "a@a.com", "x", "usuario");
    user.setId(7L);
    user.setRole(Role.SUPPORT);

    return user;
  }

  @Test
  void issuedTokenIsAcceptedByTheSameSecret() {
    JwtService service = new JwtService(SECRET, Duration.ofMinutes(15), Clock.systemUTC());

    Jwt jwt = service.decoder().decode(service.issue(user()).value());

    assertEquals("7", jwt.getSubject());
    assertEquals("usuario", jwt.getClaimAsString(JwtService.CLAIM_USERNAME));
    assertEquals("SUPPORT", jwt.getClaimAsString(JwtService.CLAIM_ROLE));
  }

  @Test
  void tokenExpiresExactlyAtTheConfiguredTime() {
    Instant now = Instant.parse("2026-01-01T12:00:00Z");
    JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(15), Clock.fixed(now, ZoneOffset.UTC));
    JwtService.AccessToken token = issuer.issue(user());

    assertEquals(now.plus(Duration.ofMinutes(15)), token.expiresAt());

    JwtService justBefore = new JwtService(SECRET, Duration.ofMinutes(15),
        Clock.fixed(now.plus(Duration.ofMinutes(15)).minusSeconds(1), ZoneOffset.UTC));
    justBefore.decoder().decode(token.value());

    JwtService justAfter = new JwtService(SECRET, Duration.ofMinutes(15),
        Clock.fixed(now.plus(Duration.ofMinutes(15)).plusSeconds(1), ZoneOffset.UTC));
    assertThrows(JwtException.class, () -> justAfter.decoder().decode(token.value()));
  }

  @Test
  void tokenFromAnotherSecretIsRejected() {
    JwtService a = new JwtService(SECRET, Duration.ofMinutes(15), Clock.systemUTC());
    JwtService b = new JwtService("fedcba9876543210fedcba9876543210", Duration.ofMinutes(15), Clock.systemUTC());

    assertThrows(JwtException.class, () -> b.decoder().decode(a.issue(user()).value()));
  }

  @Test
  void shortOrMissingSecretIsRefused() {
    assertThrows(IllegalStateException.class, () -> new JwtService("curto", Duration.ofMinutes(15), Clock.systemUTC()));
    assertThrows(IllegalStateException.class, () -> new JwtService(null, Duration.ofMinutes(15), Clock.systemUTC()));
    assertThrows(IllegalStateException.class, () -> new JwtService(SECRET, Duration.ZERO, Clock.systemUTC()));
  }

  // RefreshTokenService usa a mesma regra do JwtService
  @Test
  void refreshTokenServiceAppliesTheSameSecretRules() {
    for (String weak : new String[] { null, "curto", "a".repeat(40) })
      assertThrows(IllegalStateException.class, () -> new RefreshTokenService(
          org.mockito.Mockito.mock(com.minhascontasdb.persistence.RefreshTokenPersistence.class),
          org.mockito.Mockito.mock(com.minhascontasdb.persistence.UserPersistence.class),
          weak, Duration.ofDays(7), Duration.ofDays(30), Duration.ZERO, 10, Clock.systemUTC()));
  }

  @Test
  void repetitiveSecretIsRefusedWithoutLeakingIt() {
    for (String weak : new String[] { "a".repeat(40), "ab".repeat(20), "12345678".repeat(5) }) {
      IllegalStateException e = assertThrows(IllegalStateException.class,
          () -> new JwtService(weak, Duration.ofMinutes(15), Clock.systemUTC()));
      assertFalse(e.getMessage().contains(weak));
    }
  }

  // O exp do JWT só tem segundos inteiros: o expiresAt devolvido ao cliente tem que ser exatamente esse instante
  @Test
  void returnedExpiryMatchesTheTokenExpClaimEvenWithSubSecondClock() {
    Instant now = Instant.parse("2026-01-01T12:00:00.789Z");
    JwtService service = new JwtService(SECRET, Duration.ofMinutes(15), Clock.fixed(now, ZoneOffset.UTC));

    JwtService.AccessToken token = service.issue(user());
    Jwt jwt = service.decoder().decode(token.value());

    assertEquals(jwt.getExpiresAt(), token.expiresAt());
    assertEquals(Instant.parse("2026-01-01T12:15:00Z"), token.expiresAt());
  }
}
