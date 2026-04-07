package com.ilbo18.authrbac.global.security;

/**
 * JWT 기반 인증 사용자 정보
 */
public record AuthenticatedUser(
    Long userId,
    String loginId,
    Long roleId
) {}
