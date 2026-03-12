package com.minhascontasdb.dto;

import java.time.LocalDateTime;

public class LoginResponseDTO {

  private String token;
  private LocalDateTime expireTime;

  public LoginResponseDTO(String token, LocalDateTime expireTime) {
    this.token = token;
    this.expireTime = expireTime;
  }

  public String getToken() {
    return token;
  }

  public LocalDateTime getExpireTime() {
    return expireTime;
  }
}
