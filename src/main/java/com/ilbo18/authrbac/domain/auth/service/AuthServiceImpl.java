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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        refreshTokenStore.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpire());

        return new AuthRecord.Token(
            accessToken,
            refreshToken,
            TOKEN_TYPE,
            jwtTokenProvider.getAccessTokenExpire(),
            jwtTokenProvider.getRefreshTokenExpire(),
            user.getId(),
            user.getLoginId(),
            user.getRoleId(),
            user.getName()
        );
    }
}
