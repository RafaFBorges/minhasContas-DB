package com.minhascontasdb.dto.Errors;

public class InvalidArgumentsError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Wasn't passed a valid value.";

  public InvalidArgumentsError() {
    this("");
  }

  public InvalidArgumentsError(String adicionalMessage) {
    super((adicionalMessage != null && !adicionalMessage.isEmpty())
        ? InvalidArgumentsError.MESSAGE + ' ' + adicionalMessage
        : InvalidArgumentsError.MESSAGE);
  }
}
