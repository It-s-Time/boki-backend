package com.boki.backend.global.apiPayload.exception.handler;

import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException exception) {
        return ResponseEntity
                .status(exception.getCode().getStatus())
                .body(ApiResponse.onFailure(exception.getCode(), null));
    }

    // @RequestParam, @PathVariable 등 단일 파라미터 유효성 검증 실패 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        return ResponseEntity
                .status(GeneralErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.VALIDATION_ERROR, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
                .status(GeneralErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception
    ) {
        return ResponseEntity
                .status(GeneralErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.UNSUPPORTED_MEDIA_TYPE, null));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(Exception exception) {
        return ResponseEntity
                .status(GeneralErrorCode.NOT_FOUND.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.NOT_FOUND, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception occurred", exception);
        return ResponseEntity
                .status(GeneralErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.INTERNAL_SERVER_ERROR, null));
    }
}
