package com.minhascontasdb.dto.Errors;

public class ForbiddenError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Forbidden.";

  public ForbiddenError() {
    this("");
  }

  public ForbiddenError(String adicionalMessage) {
    super((adicionalMessage != null && !adicionalMessage.isEmpty())
        ? ForbiddenError.MESSAGE + ' ' + adicionalMessage
        : ForbiddenError.MESSAGE);
  }
}
