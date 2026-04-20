package com.ilbo18.authrbac.global.security;

import com.ilbo18.authrbac.domain.auth.entity.ExternalIdentity;
import com.ilbo18.authrbac.domain.auth.repository.ExternalIdentityRepository;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;

/**
 * Keycloak token 의 외부 role claim 을 그대로 신뢰하지 않고
 * sub 를 내부 User 와 roleId 로 다시 연결해 기존 RBAC 기준을 유지한다.
 */
@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String KEYCLOAK_PROVIDER = "KEYCLOAK";

    private final ExternalIdentityRepository externalIdentityRepository;
    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String providerUserId = jwt.getSubject();

        if (!StringUtils.hasText(providerUserId)) {
            throw authenticationException(AuthErrorCode.INVALID_TOKEN);
        }

        ExternalIdentity externalIdentity = Optional.ofNullable(externalIdentityRepository.findByProviderAndProviderUserIdAndDeletedFalseAndEnabledTrue(KEYCLOAK_PROVIDER, providerUserId))
                                                    .orElseThrow(() -> authenticationException(AuthErrorCode.EXTERNAL_IDENTITY_NOT_LINKED));

        User user = Optional.ofNullable(userRepository.findByIdAndDeletedFalse(externalIdentity.getUserId()))
                            .orElseThrow(() -> authenticationException(AuthErrorCode.EXTERNAL_IDENTITY_NOT_LINKED));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw authenticationException(AuthErrorCode.DISABLED_USER);
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            user.getId(),
            user.getLoginId(),
            user.getRoleId()
        );

        return new UsernamePasswordAuthenticationToken(authenticatedUser, jwt, Collections.emptyList());
    }

    private OAuth2AuthenticationException authenticationException(AuthErrorCode errorCode) {
        OAuth2Error error = new OAuth2Error(errorCode.getCode(), errorCode.getMessage(), null);
        return new OAuth2AuthenticationException(error);
    }
}
