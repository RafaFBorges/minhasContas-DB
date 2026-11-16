package com.minhascontasdb.dto.Errors;

public class TooManyRequestsError extends ErrorResponseDTOObject {
  public static final String MESSAGE = "Too many attempts. Try again later.";

  private final long retryAfterSeconds;

  public TooManyRequestsError(long retryAfterSeconds) {
    super(MESSAGE);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
