package com.ilbo18.authrbac.global.exception;

/**
 * 서비스 전역에서 사용하는 사용자 정의 예외
 */
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
