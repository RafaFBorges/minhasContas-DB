package com.minhascontasdb.dto;

public class RegisterResponseDTO {

  private boolean isCreated;

  public RegisterResponseDTO(boolean isCreated) {
    this.isCreated = isCreated;
  }

  public boolean getIsCreated() {
    return this.isCreated;
  }
}
