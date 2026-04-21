package com.minhascontasdb.dto.Errors;

public class NotFoundError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Wasn't found the required resource.";

  public NotFoundError() {
    this("");
  }

  public NotFoundError(String adicionalMessage) {
    super((adicionalMessage != "")
        ? NotFoundError.MESSAGE + ' ' + adicionalMessage
        : NotFoundError.MESSAGE);
  }
}
