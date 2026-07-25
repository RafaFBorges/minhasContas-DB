package com.minhascontasdb.dto.Errors;

public class InvalidAccessError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Invalid access.";

  public InvalidAccessError() {
    this(InvalidAccessError.MESSAGE);
  }

  public InvalidAccessError(String adicionalMessage) {
    super((adicionalMessage != "")
        ? InvalidAccessError.MESSAGE + ' ' + adicionalMessage
        : InvalidAccessError.MESSAGE);
  }
}
