package com.ilbo18.authrbac.domain.auth.service;

import com.ilbo18.authrbac.domain.auth.record.AuthRecord;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.security.AuthenticatedUser;
import com.ilbo18.authrbac.global.security.JwtTokenProvider;
import com.ilbo18.authrbac.global.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * access token은 stateless로 유지하고, refresh token만 Redis에서 회전시킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional
    public AuthRecord.Token login(AuthRecord.Login req) {
        String loginId = TextNormalizer.trimToLowerCase(req.loginId());

        User user = Optional.ofNullable(userRepository.findByLoginIdAndDeletedFalse(loginId))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_LOGIN));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new CustomException(AuthErrorCode.DISABLED_USER);
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_LOGIN);
        }

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthRecord.Token reissue(AuthRecord.Reissue req) {
        Long userId = refreshTokenStore.findUserId(req.refreshToken());

        if (userId == null || !refreshTokenStore.isCurrentToken(userId, req.refreshToken())) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = Optional.ofNullable(userRepository.findByIdAndDeletedFalse(userId))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new CustomException(AuthErrorCode.DISABLED_USER);
        }

        refreshTokenStore.deleteByUserId(userId);

        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new CustomException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        refreshTokenStore.deleteByUserId(authenticatedUser.userId());
    }

    @Override
    public AuthRecord.Me getMe(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new CustomException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        return new AuthRecord.Me(
            authenticatedUser.userId(),
            authenticatedUser.loginId(),
            authenticatedUser.roleId()
        );
    }

    private AuthRecord.Token issueTokens(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            user.getId(),
            user.getLoginId(),
            user.getRoleId()
        );

        String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        refreshTokenStore.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpiresIn());

        return new AuthRecord.Token(
            accessToken,
            refreshToken,
            TOKEN_TYPE,
            jwtTokenProvider.getAccessTokenExpiresIn(),
            jwtTokenProvider.getRefreshTokenExpiresIn(),
            user.getId(),
            user.getLoginId(),
            user.getRoleId(),
            user.getName()
        );
    }
}

/**
 * 사용자당 최신 refresh token 1개만 유지하면 rotation과 logout을 가장 짧게 설명할 수 있다.
 */
@Component
@RequiredArgsConstructor
class RefreshTokenStore {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:user:";

    private final StringRedisTemplate stringRedisTemplate;

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

    public boolean isCurrentToken(Long userId, String refreshToken) {
        String currentToken = stringRedisTemplate.opsForValue().get(getUserKey(userId));

        return refreshToken.equals(currentToken);
    }

    public void deleteByUserId(Long userId) {
        String userKey = getUserKey(userId);
        String refreshToken = stringRedisTemplate.opsForValue().get(userKey);

        if (StringUtils.hasText(refreshToken)) {
            stringRedisTemplate.delete(getRefreshTokenKey(refreshToken));
        }

        stringRedisTemplate.delete(userKey);
    }

    private String getRefreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_KEY_PREFIX + refreshToken;
    }

    private String getUserKey(Long userId) {
        return USER_REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}
