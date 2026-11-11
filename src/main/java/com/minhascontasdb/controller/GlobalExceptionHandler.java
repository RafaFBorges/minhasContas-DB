package com.minhascontasdb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.dto.Errors.ForbiddenError;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.dto.Errors.NotFoundError;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(InvalidAccessError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidAccess(InvalidAccessError e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getResponse());
  }

  // Pedido sem o header "token": é falta de autenticação, não erro do servidor
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponseDTO> handleMissingHeader(MissingRequestHeaderException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new InvalidAccessError("Missing token").getResponse());
  }

  @ExceptionHandler(ForbiddenError.class)
  public ResponseEntity<ErrorResponseDTO> handleForbidden(ForbiddenError e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getResponse());
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

  // Corpo que não é um JSON válido
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(new ErrorResponseDTO("Malformed request body"));
  }

  // Parâmetro de rota ou de query com tipo errado (ex.: /expense/abc)
  @ExceptionHandler(TypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(TypeMismatchException e) {
    return ResponseEntity.badRequest().body(new ErrorResponseDTO("Invalid request parameter"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception e) {
    // As exceções do próprio Spring MVC (rota inexistente, método ou content-type não suportado...) já
    // trazem o status certo: não podem virar 500
    if (e instanceof ErrorResponse frameworkError && frameworkError.getStatusCode().is4xxClientError()) {
      HttpStatusCode status = frameworkError.getStatusCode();
      HttpStatus known = HttpStatus.resolve(status.value());

      return ResponseEntity.status(status)
          .headers(frameworkError.getHeaders())
          .body(new ErrorResponseDTO(known != null ? known.getReasonPhrase() : "Request error"));
    }

    // O detalhe fica só no log: a mensagem da exceção pode expor SQL, caminhos ou outros dados internos
    log.error("Unhandled exception", e);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDTO("Internal server error"));
  }
}
