package com.minhascontasdb.dto.Errors;

public class InvalidArgumentsError extends ErrorResponseDTO {
  public static final String MESSAGE = "Wasn't passed a valid value.";

  public InvalidArgumentsError() {
    super(InvalidArgumentsError.MESSAGE);
  }
}
