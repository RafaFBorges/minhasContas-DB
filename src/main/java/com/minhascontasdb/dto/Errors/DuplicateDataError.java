package com.minhascontasdb.dto.Errors;

public class DuplicateDataError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Duplicate data.";

  public DuplicateDataError() {
    this("");
  }

  public DuplicateDataError(String adicionalMessage) {
    super((adicionalMessage != null && !adicionalMessage.isEmpty())
        ? DuplicateDataError.MESSAGE + ' ' + adicionalMessage
        : DuplicateDataError.MESSAGE);
  }
}
