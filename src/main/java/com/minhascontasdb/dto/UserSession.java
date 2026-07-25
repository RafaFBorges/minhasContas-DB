package com.minhascontasdb.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record UserSession(String token, Instant expiresAt, Long id, String user, String name) {
  public static UserSession create(Long id, String user, String name) {
    return new UserSession(
        UUID.randomUUID().toString().replace("-", ""),
        Instant.now().plus(1, ChronoUnit.HOURS),
        id,
        user,
        name);
  }

  public boolean isExpired() {
    return Instant.now().isAfter(this.expiresAt);
  }
}
