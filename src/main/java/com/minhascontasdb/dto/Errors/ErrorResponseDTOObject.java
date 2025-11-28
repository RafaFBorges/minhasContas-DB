package com.minhascontasdb.dto.Errors;

public class ErrorResponseDTOObject extends RuntimeException {
  private String message;

  public ErrorResponseDTOObject(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ErrorResponseDTO getResponse() {
    return new ErrorResponseDTO(this.message);
  }
}
