package com.ilbo18.authrbac.domain.auth.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 관련 DTO 모음
 */
public class AuthRecord {

    /** 로그인 요청 */
    public record Login(
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 50, message = "로그인 ID는 50자 이하여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 255, message = "비밀번호는 255자 이하여야 합니다.")
        String password
    ) {}

    /** 토큰 응답 */
    public record Token(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String loginId,
        Long roleId,
        String name
    ) {}

    /** 인증 사용자 응답 */
    public record Me(
        Long userId,
        String loginId,
        Long roleId
    ) {}
}
