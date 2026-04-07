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
 * 인증 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthRecord.Token login(AuthRecord.Login req) {
        String loginId = TextNormalizer.trimToLowerCase(req.loginId());

        User user = Optional.ofNullable(userRepository.findByLoginIdAndDeletedFalse(loginId))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_LOGIN));

        if (!Boolean.TRUE.equals(user.getEnabled())) throw new CustomException(AuthErrorCode.DISABLED_USER);
        if (!passwordEncoder.matches(req.password(), user.getPassword())) throw new CustomException(AuthErrorCode.INVALID_LOGIN);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getLoginId(), user.getRoleId());

        String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);

        return new AuthRecord.Token(
            accessToken,
            TOKEN_TYPE,
            jwtTokenProvider.getAccessTokenExpiresIn(),
            user.getId(),
            user.getLoginId(),
            user.getRoleId(),
            user.getName()
        );
    }

    @Override
    public AuthRecord.Me getMe(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) throw new CustomException(AuthErrorCode.AUTHENTICATION_REQUIRED);

        return new AuthRecord.Me(
            authenticatedUser.userId(),
            authenticatedUser.loginId(),
            authenticatedUser.roleId()
        );
    }
}
