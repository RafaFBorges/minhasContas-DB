package com.minhascontasdb.dto;

import java.time.Instant;

public class LoginResponseDTO {

  private String token;
  private Instant expiresAt;
  private String user;
  private Long id;
  private String name;

  public LoginResponseDTO() {
  }

  public LoginResponseDTO(String token, Instant expiresAt, Long id, String user, String name) {
    this.token = token;
    this.expiresAt = expiresAt;
    this.user = user;
    this.id = id;
    this.name = name;
  }

  public LoginResponseDTO(String token, Instant expiresAt) {
    this(token, expiresAt, null, null, null);
  }

  public String getToken() {
    return token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getUser() {
    return user;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }
}
