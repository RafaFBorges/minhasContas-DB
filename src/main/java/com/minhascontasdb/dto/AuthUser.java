package com.minhascontasdb.dto;

import com.minhascontasdb.service.Role;

// Quem está fazendo a requisição, extraído do access token (JWT) já validado.
// Os controllers recebem com @AuthenticationPrincipal. O papel vale até o token expirar (15 min)
public record AuthUser(Long id, String user, Role role) {
}
