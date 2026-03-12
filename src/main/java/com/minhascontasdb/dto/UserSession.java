package com.minhascontasdb.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSession(String token, LocalDateTime expiresAt) {
    public static UserSession create() {
        return new UserSession(
            UUID.randomUUID().toString().replace("-", ""),
            LocalDateTime.now().plusHours(1)
        );
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
