package com.ssafy.ssafit.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ssafy.ssafit.common.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.name(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception) {
        FieldError firstFieldError = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .orElse(null);
        ErrorCode errorCode = validationErrorCode(firstFieldError);
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(errorCode.getMessage());
        if (errorCode != ErrorCode.INVALID_REQUEST) {
            message = errorCode.getMessage();
        }
        ErrorResponse response = new ErrorResponse(errorCode.name(), message);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    private ErrorCode validationErrorCode(FieldError fieldError) {
        if (fieldError == null) {
            return ErrorCode.INVALID_REQUEST;
        }
        return switch (fieldError.getField()) {
            case "targetCadenceMin", "targetCadenceMax", "targetCadenceSpm",
                    "averageCadence" ->
                    ErrorCode.INVALID_CADENCE_VALUE;
            case "averageHeartRateBpm" -> ErrorCode.INVALID_HEART_RATE_VALUE;
            case "musicBpm" -> ErrorCode.INVALID_MUSIC_BPM_VALUE;
            default -> ErrorCode.INVALID_REQUEST;
        };
    }

    @ExceptionHandler({
            BindException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ErrorResponse response = new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(
                        "METHOD_NOT_ALLOWED",
                        "지원하지 않는 HTTP 메서드입니다."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "RESOURCE_NOT_FOUND",
                        "요청한 API 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception) {
        log.error("Unexpected API error", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(
                        errorCode.name(),
                        errorCode.getMessage()));
    }
}
