package com.ilbo18.authrbac.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 사용자당 최신 refresh token 1개만 유지하면 rotation과 logout을 가장 짧게 설명할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:user:";

    private final StringRedisTemplate stringRedisTemplate;

    /** 사용자 최신 refresh token 을 저장한다. */
    public void save(Long userId, String refreshToken, long ttlSeconds) {
        String userKey = getUserKey(userId);
        String previousToken = stringRedisTemplate.opsForValue().get(userKey);

        if (StringUtils.hasText(previousToken)) {
            stringRedisTemplate.delete(getRefreshTokenKey(previousToken));
        }

        Duration ttl = Duration.ofSeconds(ttlSeconds);

        stringRedisTemplate.opsForValue().set(getRefreshTokenKey(refreshToken), userId.toString(), ttl);
        stringRedisTemplate.opsForValue().set(userKey, refreshToken, ttl);
    }

    /** refresh token 으로 사용자 ID 를 조회한다. */
    public Long findUserId(String refreshToken) {
        String userId = stringRedisTemplate.opsForValue().get(getRefreshTokenKey(refreshToken));

        if (!StringUtils.hasText(userId)) {
            return null;
        }

        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 현재 사용자의 최신 refresh token 인지 확인한다. */
    public boolean isCurrentToken(Long userId, String refreshToken) {
        String currentToken = stringRedisTemplate.opsForValue().get(getUserKey(userId));

        return refreshToken.equals(currentToken);
    }

    /** 사용자 기준으로 refresh token 키를 함께 삭제한다. */
    public void deleteByUserId(Long userId) {
        String userKey = getUserKey(userId);
        String refreshToken = stringRedisTemplate.opsForValue().get(userKey);

        if (StringUtils.hasText(refreshToken)) {
            stringRedisTemplate.delete(getRefreshTokenKey(refreshToken));
        }

        stringRedisTemplate.delete(userKey);
    }

    /** token 기준 Redis 키를 생성한다. */
    private String getRefreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_KEY_PREFIX + refreshToken;
    }

    /** user 기준 Redis 키를 생성한다. */
    private String getUserKey(Long userId) {
        return USER_REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}
