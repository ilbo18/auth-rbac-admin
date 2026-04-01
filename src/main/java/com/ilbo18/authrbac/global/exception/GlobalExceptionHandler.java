package com.ilbo18.authrbac.global.exception;

import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.response.ErrorResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스에서 직접 던진 CustomException 처리 */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseRecord> handleCustomException(CustomException e) {
        log.warn("[CustomException] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());

        return ErrorResponseRecord.toResponseEntity(e);
    }

    /** @Valid 검증 실패 처리 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseRecord> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                          .getFieldErrors()
                          .stream()
                          .findFirst()
                          .map(FieldError::getDefaultMessage)
                          .orElse(AuthErrorCode.VALIDATION_ERROR.getMessage());
        log.warn("[ValidationError] message={}", message);

        return ErrorResponseRecord.toResponseEntity(AuthErrorCode.VALIDATION_ERROR, message);
    }

    /** JSON 형식 오류 처리 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseRecord> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[MalformedJson] message={}", e.getMessage());

        return ErrorResponseRecord.toResponseEntity(AuthErrorCode.MALFORMED_JSON);
    }

    /** 처리되지 않은 예외 처리 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseRecord> handleException(Exception e) {
        log.error("[UnhandledException] message={}", e.getMessage(), e);

        return ErrorResponseRecord.toResponseEntity(AuthErrorCode.INTERNAL_SERVER_ERROR);
    }
}
