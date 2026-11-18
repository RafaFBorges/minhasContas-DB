package com.minhascontasdb.service;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Só o hash do token fica no banco: um vazamento dele não permite usar nenhuma sessão
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "chain_id", nullable = false)
  private UUID chainId;

  @Column(name = "chain_started_at", nullable = false)
  private Instant chainStartedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public RefreshToken() {
  }

  public RefreshToken(Long userId, String tokenHash, UUID chainId, Instant chainStartedAt, Instant createdAt,
      Instant expiresAt) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.chainId = chainId;
    this.chainStartedAt = chainStartedAt;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public UUID getChainId() {
    return chainId;
  }

  public Instant getChainStartedAt() {
    return chainStartedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }
}
