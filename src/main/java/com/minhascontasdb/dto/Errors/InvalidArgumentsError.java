package com.minhascontasdb.dto.Errors;

public class InvalidArgumentsError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Wasn't passed a valid value.";

  public InvalidArgumentsError() {
    this("");
  }

  public InvalidArgumentsError(String adicionalMessage) {
    super((adicionalMessage != "")
        ? InvalidArgumentsError.MESSAGE + ' ' + adicionalMessage
        : InvalidArgumentsError.MESSAGE);
  }
}
