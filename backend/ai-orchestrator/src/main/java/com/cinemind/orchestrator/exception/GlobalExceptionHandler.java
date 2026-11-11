package com.cinemind.orchestrator.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinemind.orchestrator.dto.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleLlm(LlmUnavailableException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(RagUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleRag(RagUnavailableException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidPreferencesException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPreferences(InvalidPreferencesException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request validation failed",
                fieldErrors(ex.getBindingResult()), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON",
                Map.of(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode code = errorResponse.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value()) != null
                    ? HttpStatus.valueOf(code.value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            ProblemDetail problem = errorResponse.getBody();
            String message = problem.getDetail() != null ? problem.getDetail() : status.getReasonPhrase();
            return build(status, message, Map.of(), request);
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                Map.of(), request);
    }

    private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message,
                                                          Map<String, String> fieldErrors,
                                                          HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now().toString(), status.value(), status.getReasonPhrase(),
                message == null ? status.getReasonPhrase() : message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private static Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }
}
