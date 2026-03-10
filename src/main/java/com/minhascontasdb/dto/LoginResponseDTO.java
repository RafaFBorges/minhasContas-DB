package com.minhascontasdb.dto;

import java.time.LocalDateTime;

public class LoginResponseDTO {

  private String token;
  private LocalDateTime expireTime;

  public LoginResponseDTO(String token) {
    this.token = token;
    this.expireTime = LocalDateTime.now().plusHours(1);
  }

  public String getToken() {
    return token;
  }

  public LocalDateTime getExpireTime() {
    return expireTime;
  }
}
