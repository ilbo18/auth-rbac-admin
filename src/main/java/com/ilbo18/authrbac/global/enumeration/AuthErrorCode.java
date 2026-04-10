package com.ilbo18.authrbac.global.enumeration;

import com.ilbo18.authrbac.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * auth-rbac-admin 프로젝트 공용 에러 코드
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
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A3004", "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A3005", "만료된 인증 토큰입니다."),
    DISABLED_USER(HttpStatus.FORBIDDEN, "A3006", "비활성화된 사용자입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "A3007", "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A3008", "유효하지 않은 refresh token입니다."),
    EXTERNAL_IDENTITY_NOT_LINKED(HttpStatus.UNAUTHORIZED, "A3009", "연결되지 않은 외부 인증 사용자입니다."),

    INVALID_ROLE(HttpStatus.BAD_REQUEST, "A4001", "유효하지 않은 역할입니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "A4002", "역할 정보를 찾을 수 없습니다."),
    ROLE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A4003", "이미 존재하는 역할 코드입니다."),

    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "A5001", "권한 정보를 찾을 수 없습니다."),
    PERMISSION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A5002", "이미 존재하는 권한 조합입니다."),
    INVALID_PERMISSION_ACTION(HttpStatus.BAD_REQUEST, "A5003", "최소 하나 이상의 권한이 필요합니다."),

    INVALID_MENU(HttpStatus.BAD_REQUEST, "A6001", "유효하지 않은 메뉴입니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "A6002", "메뉴 정보를 찾을 수 없습니다."),
    MENU_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A6003", "이미 존재하는 메뉴 경로입니다."),
    INVALID_PARENT_MENU(HttpStatus.BAD_REQUEST, "A6004", "유효하지 않은 상위 메뉴입니다."),

    AUDIT_NOT_FOUND(HttpStatus.NOT_FOUND, "A7001", "감사 로그를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
