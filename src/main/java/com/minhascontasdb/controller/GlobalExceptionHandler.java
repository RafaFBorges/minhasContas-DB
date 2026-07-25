package com.minhascontasdb.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.dto.Errors.NotFoundError;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidAccessError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidAccess(InvalidAccessError e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getResponse());
  }

  @ExceptionHandler(DuplicateDataError.class)
  public ResponseEntity<ErrorResponseDTO> handleDuplicateDataError(DuplicateDataError e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getResponse());
  }

  @ExceptionHandler(InvalidArgumentsError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidArguments(InvalidArgumentsError e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(e.getMessage()));
  }

  @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidDataAccessResourceUsage(InvalidDataAccessResourceUsageException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InvalidArgumentsError("A column was not found").getResponse());
  }

  @ExceptionHandler(NotFoundError.class)
  public ResponseEntity<ErrorResponseDTO> handleNotFoundError(NotFoundError e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getResponse());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException e) {
    return ResponseEntity.badRequest().body(new InvalidArgumentsError(e.getMessage()).getResponse());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDTO("Error=" + e.getMessage()));
  }
}
