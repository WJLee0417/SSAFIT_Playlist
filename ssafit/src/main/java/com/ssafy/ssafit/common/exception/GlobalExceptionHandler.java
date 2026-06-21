package com.ssafy.ssafit.common.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ssafy.ssafit.common.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.name(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(errorCode.getMessage());
        ErrorResponse response = new ErrorResponse(errorCode.name(), message);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
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
}
