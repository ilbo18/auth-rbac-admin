package com.ilbo18.authrbac.domain.auth.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 관련 DTO 모음
 */
public class AuthRecord {

    public record Login(
        @NotBlank(message = "loginId는 필수입니다.")
        @Size(max = 50, message = "loginId는 50자 이하여야 합니다.")
        String loginId,

        @NotBlank(message = "password는 필수입니다.")
        @Size(max = 255, message = "password는 255자 이하여야 합니다.")
        String password
    ) {}

    public record Reissue(
        @NotBlank(message = "refreshToken은 필수입니다.")
        @Size(max = 255, message = "refreshToken은 255자 이하여야 합니다.")
        String refreshToken
    ) {}

    public record Token(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        Long userId,
        String loginId,
        Long roleId,
        String name
    ) {}

    public record Me(
        Long userId,
        String loginId,
        Long roleId
    ) {}
}
