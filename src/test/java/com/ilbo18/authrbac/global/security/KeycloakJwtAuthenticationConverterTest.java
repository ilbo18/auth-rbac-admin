package com.ilbo18.authrbac.global.security;

import com.ilbo18.authrbac.domain.auth.entity.ExternalIdentity;
import com.ilbo18.authrbac.domain.auth.repository.ExternalIdentityRepository;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Keycloak token 을 내부 User 로 다시 연결하는 규칙만 단위 테스트로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakJwtAuthenticationConverterTest {

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void ExternalIdentity_매핑이_있으면_AuthenticatedUser로_변환한다() {
        // given
        Jwt jwt = createJwt("keycloak-user-1");
        ExternalIdentity externalIdentity = ExternalIdentity.builder()
                                                            .id(1L)
                                                            .userId(10L)
                                                            .provider("KEYCLOAK")
                                                            .providerUserId("keycloak-user-1")
                                                            .enabled(true)
                                                            .build();
        User user = User.builder()
                        .id(10L)
                        .loginId("kcadmin")
                        .password("encoded")
                        .name("Keycloak Admin")
                        .roleId(3L)
                        .enabled(true)
                        .build();

        when(externalIdentityRepository.findByProviderAndProviderUserIdAndDeletedFalseAndEnabledTrue("KEYCLOAK", "keycloak-user-1"))
            .thenReturn(externalIdentity);
        when(userRepository.findByIdAndDeletedFalse(10L)).thenReturn(user);

        // when
        UsernamePasswordAuthenticationToken authentication =
            (UsernamePasswordAuthenticationToken) keycloakJwtAuthenticationConverter.convert(jwt);

        // then
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

        assertThat(authenticatedUser.userId()).isEqualTo(10L);
        assertThat(authenticatedUser.loginId()).isEqualTo("kcadmin");
        assertThat(authenticatedUser.roleId()).isEqualTo(3L);
    }

    @Test
    void ExternalIdentity_매핑이_없으면_인증에_실패한다() {
        // given
        Jwt jwt = createJwt("missing-user");

        when(externalIdentityRepository.findByProviderAndProviderUserIdAndDeletedFalseAndEnabledTrue("KEYCLOAK", "missing-user"))
            .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> keycloakJwtAuthenticationConverter.convert(jwt))
            .isInstanceOf(OAuth2AuthenticationException.class)
            .extracting(exception -> ((OAuth2AuthenticationException) exception).getError().getErrorCode())
            .isEqualTo("A3009");
    }

    private Jwt createJwt(String subject) {
        return Jwt.withTokenValue("token")
                  .header("alg", "RS256")
                  .claim("sub", subject)
                  .subject(subject)
                  .build();
    }
}
