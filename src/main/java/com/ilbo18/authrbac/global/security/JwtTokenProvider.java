package com.ilbo18.authrbac.global.security;

import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT access token 생성과 검증을 담당한다.
 */
@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret-key}")
    private String secretKeyValue;

    @Value("${security.jwt.access-token-expiration-seconds}")
    private long accessTokenExpirationSeconds;

    private SecretKey secretKey;

    /** JWT 서명 키를 초기화한다. */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
    }

    /** access token을 생성한다. */
    public String createAccessToken(AuthenticatedUser authenticatedUser) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                   .subject(authenticatedUser.loginId())
                   .claim("userId", authenticatedUser.userId())
                   .claim("loginId", authenticatedUser.loginId())
                   .claim("roleId", authenticatedUser.roleId())
                   .issuedAt(Date.from(now))
                   .expiration(Date.from(expiresAt))
                   .signWith(secretKey)
                   .compact();
    }

    /** access token 만료 시간을 초 단위로 반환한다. */
    public long getAccessTokenExpiresIn() {
        return accessTokenExpirationSeconds;
    }

    /** 토큰에서 인증 사용자 정보를 추출한다. */
    public AuthenticatedUser getAuthenticatedUser(String token) {
        try {
            Claims claims = Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();

            Long userId = claims.get("userId", Long.class);
            String loginId = claims.get("loginId", String.class);
            Long roleId = claims.get("roleId", Long.class);

            if (userId == null || loginId == null || loginId.isBlank() || roleId == null) throw new CustomException(AuthErrorCode.INVALID_TOKEN);

            return new AuthenticatedUser(userId, loginId, roleId);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}