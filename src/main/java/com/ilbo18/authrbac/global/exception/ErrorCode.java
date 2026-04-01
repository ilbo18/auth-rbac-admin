package com.ilbo18.authrbac.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드 규약
 */
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
    String name();
}
