package com.ilbo18.authrbac.global.response;

import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;

/**
 * API 실패 응답 공통 구조
 */
public record ErrorResponseRecord(
        int status,
        String statusText,
        String code,
        String msg
) {

    /** ErrorCode 기반 실패 응답 생성 */
    public static ErrorResponseRecord of(ErrorCode errorCode) {
        return new ErrorResponseRecord(
                errorCode.getStatus().value(),
                errorCode.getStatus().getReasonPhrase(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }

    /** ErrorCode + 커스텀 메시지 기반 실패 응답 생성 */
    public static ErrorResponseRecord of(ErrorCode errorCode, String message) {
        return new ErrorResponseRecord(
                errorCode.getStatus().value(),
                errorCode.getStatus().getReasonPhrase(),
                errorCode.getCode(),
                message
        );
    }

    /** ErrorCode 기반 ResponseEntity 반환 */
    public static ResponseEntity<ErrorResponseRecord> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                             .body(of(errorCode));
    }

    /** ErrorCode + 커스텀 메시지 기반 ResponseEntity 반환 */
    public static ResponseEntity<ErrorResponseRecord> toResponseEntity(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                             .body(of(errorCode, message));
    }

    /** CustomException 기반 ResponseEntity 반환 */
    public static ResponseEntity<ErrorResponseRecord> toResponseEntity(CustomException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                             .body(of(e.getErrorCode()));
    }
}
