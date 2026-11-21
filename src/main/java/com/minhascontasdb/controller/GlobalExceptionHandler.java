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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.minhascontasdb.dto.Errors.DuplicateDataError;
import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.dto.Errors.ForbiddenError;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.dto.Errors.NotFoundError;
import com.minhascontasdb.dto.Errors.TooManyRequestsError;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(InvalidAccessError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidAccess(InvalidAccessError e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getResponse());
  }

  @ExceptionHandler(TooManyRequestsError.class)
  public ResponseEntity<ErrorResponseDTO> handleTooManyRequests(TooManyRequestsError e) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
        .body(e.getResponse());
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
    // A mensagem vem de bibliotecas (Hibernate, Jackson...) e pode expor nomes internos: o detalhe fica só no log
    log.warn("Illegal argument", e);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new InvalidArgumentsError().getResponse());
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
    // A mensagem traz SQL, nome de tabela, de constraint e os valores enviados: o detalhe fica só no log
    log.warn("Data integrity violation", e);

    return ResponseEntity.badRequest().body(new InvalidArgumentsError().getResponse());
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

  // Regra de papel/acesso lançada dentro de um controller (@PreAuthorize, hasRole...): o catch-all abaixo a transformaria em 500.
  // Mesmo corpo que o SecurityConfig devolve para quem é barrado antes do controller. Ao escrever a primeira regra de papel,
  // conferir aqui (e em SecurityExceptionHandlingTests) o comportamento desejado
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ForbiddenError().getResponse());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException e) {
    // O normal é o filtro do Spring Security responder antes (com error="invalid_token" quando há token recusado); isto é só o caminho
    // de quem lança AuthenticationException dentro de um controller
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("WWW-Authenticate", "Bearer")
        .body(new InvalidAccessError().getResponse());
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
