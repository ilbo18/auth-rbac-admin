package com.ilbo18.authrbac.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * 이 provider 는 local mode 의 JWT 발급과 검증만 담당한다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.jwt.issuer}")
    private String issuer;

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
                   .issuer(issuer)
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

    public long getAccessTokenExpire() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpire() {
        return refreshTokenExpirationSeconds;
    }

    public boolean isLocalToken(String token) {
        try {
            String[] tokenParts = token.split("\\.");

            if (tokenParts.length != 3) {
                return false;
            }

            JsonNode payload = objectMapper.readTree(Decoders.BASE64URL.decode(tokenParts[1]));

            return issuer.equals(payload.path("iss").asText());
        } catch (Exception e) {
            return false;
        }
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

            if (!issuer.equals(claims.getIssuer())) {
                throw new CustomException(AuthErrorCode.INVALID_TOKEN);
            }

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
