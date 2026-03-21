package com.minhascontasdb.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSession(String token, LocalDateTime expiresAt, Long id, String user, String name) {
  public static UserSession create(Long id, String user, String name) {
    return new UserSession(
        UUID.randomUUID().toString().replace("-", ""),
        LocalDateTime.now().plusHours(1),
        id,
        user,
        name);
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }
}
