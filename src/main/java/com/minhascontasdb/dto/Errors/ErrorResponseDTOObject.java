package com.minhascontasdb.dto.Errors;

public class ErrorResponseDTOObject extends RuntimeException {
  public ErrorResponseDTOObject(String message) {
    super(message);
  }

  public ErrorResponseDTO getResponse() {
    return new ErrorResponseDTO(this.getMessage());
  }
}
