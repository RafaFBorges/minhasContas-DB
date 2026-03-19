package com.minhascontasdb.dto;

import java.time.LocalDateTime;

public class LoginResponseDTO {

  private String token;
  private LocalDateTime expireTime;
  private String user;
  private Long id;
  private String name;

  public LoginResponseDTO(String token, LocalDateTime expireTime, Long id, String user, String name) {
    this.token = token;
    this.expireTime = expireTime;
    this.user = user;
    this.id = id;
    this.name = name;
  }

  public LoginResponseDTO(String token, LocalDateTime expireTime) {
    this(token, expireTime, null, null, null);
  }

  public String getToken() {
    return token;
  }

  public LocalDateTime getExpireTime() {
    return expireTime;
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
}
