package com.larva.cashback.global.exception;

import com.larva.cashback.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] code: {}, message: {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();
        log.warn("[ValidationException] message: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("INVALID_INPUT", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("[Exception] message: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }

}
