package com.minhascontasdb.dto;

public class LoginRequestDTO {

  private String user;
  private String password;

  public LoginRequestDTO(String user, String password) {
    this.user = user;
    this.password = password;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }
}
