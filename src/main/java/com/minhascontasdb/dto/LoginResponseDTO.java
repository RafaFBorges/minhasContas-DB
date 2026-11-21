package com.minhascontasdb.dto;

import java.time.Instant;

import com.minhascontasdb.service.Role;

// Resposta de /login e /refresh. Quem chama (o proxy do Web) guarda os dois tokens; o navegador nunca os lê
public record LoginResponseDTO(
    String accessToken,
    Instant expiresAt,
    String refreshToken,
    Instant refreshExpiresAt,
    Long id,
    String user,
    String name,
    Role role) {
}
