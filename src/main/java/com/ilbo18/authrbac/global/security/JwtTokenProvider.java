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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * access token은 JWT로 유지하고, refresh token은 Redis에 저장할 랜덤 문자열로 분리한다.
 */
@Component
public class JwtTokenProvider {

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.jwt.secret-key}")
    private String secretKeyValue;

    @Value("${security.jwt.access-token-expiration-seconds}")
    private long accessTokenExpirationSeconds;

    @Value("${security.jwt.refresh-token-expiration-seconds}")
    private long refreshTokenExpirationSeconds;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
    }

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

    public String createRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(randomBytes);
    }

    public long getAccessTokenExpiresIn() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpiresIn() {
        return refreshTokenExpirationSeconds;
    }

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

            if (userId == null || loginId == null || loginId.isBlank() || roleId == null) {
                throw new CustomException(AuthErrorCode.INVALID_TOKEN);
            }

            return new AuthenticatedUser(userId, loginId, roleId);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
