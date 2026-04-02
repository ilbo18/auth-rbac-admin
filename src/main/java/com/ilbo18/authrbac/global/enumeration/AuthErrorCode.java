package com.ilbo18.authrbac.global.enumeration;

import com.ilbo18.authrbac.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * auth-rbac-admin 프로젝트 전용 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "A1000", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A1001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A1002", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "A1003", "요청한 데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A1004", "서버 내부 오류가 발생했습니다."),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "A2001", "요청값 검증에 실패했습니다."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "A2002", "올바르지 않은 JSON 요청입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A3001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A3002", "이미 존재하는 사용자입니다."),
    INVALID_LOGIN(HttpStatus.BAD_REQUEST, "A3003", "아이디 또는 비밀번호가 올바르지 않습니다."),

    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "A4001", "역할 정보를 찾을 수 없습니다."),
    ROLE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A4002", "이미 존재하는 역할 코드입니다."),

    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "A5001", "권한 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
