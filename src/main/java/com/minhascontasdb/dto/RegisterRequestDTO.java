package com.minhascontasdb.dto;

public class RegisterRequestDTO {
  private String name;
  private String email;
  private String user;
  private String password;

  public RegisterRequestDTO(String name, String email, String password, String user) {
    this.name = name;
    this.email = email;
    this.user = user;
    this.password = password;
  }

  public String getName() {
    return this.name;
  }

  public String getEmail() {
    return this.email;
  }

  public String getPassword() {
    return this.password;
  }

  public String getUser() {
    return this.user;
  }

  public void setName(String newName) {
    this.name = newName;
  }

  public void setEmail(String newEmail) {
    this.email = newEmail;
  }

  public void setPassword(String newPassword) {
    this.password = newPassword;
  }

  public void setUser(String newUser) {
    this.user = newUser;
  }
}
