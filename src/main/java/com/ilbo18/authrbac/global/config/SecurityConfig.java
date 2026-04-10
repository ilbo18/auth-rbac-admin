package com.ilbo18.authrbac.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.ErrorCode;
import com.ilbo18.authrbac.global.response.ErrorResponseRecord;
import com.ilbo18.authrbac.global.security.ApiAuthorizationFilter;
import com.ilbo18.authrbac.global.security.JwtAuthenticationFilter;
import com.ilbo18.authrbac.global.security.JwtTokenProvider;
import com.ilbo18.authrbac.global.security.KeycloakJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * logout은 인증된 사용자 기준으로 처리해 refresh token을 body로 다시 받지 않는다.
 */
@Configuration
public class SecurityConfig {

    private static final String LOCAL_MODE = "local";
    private static final String KEYCLOAK_MODE = "keycloak";

    @Value("${auth.mode:local}")
    private String authMode;

    @Value("${security.keycloak.enabled:false}")
    private boolean keycloakEnabled;

    @Bean
    @ConditionalOnProperty(prefix = "security.keycloak", name = "enabled", havingValue = "true")
    public JwtDecoder jwtDecoder(@Value("${security.keycloak.issuer-uri}") String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver(JwtTokenProvider jwtTokenProvider) {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

        return request -> {
            String token = delegate.resolve(request);

            if (!StringUtils.hasText(token) || jwtTokenProvider.isLocalToken(token)) {
                return null;
            }

            return token;
        };
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        ApiAuthorizationFilter apiAuthorizationFilter,
        KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter,
        BearerTokenResolver bearerTokenResolver,
        ObjectMapper objectMapper
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, ex) -> writeErrorResponse(response, objectMapper, resolveAuthenticationErrorCode(ex)))
                .accessDeniedHandler((request, response, ex) -> writeErrorResponse(response, objectMapper, AuthErrorCode.FORBIDDEN))
            )
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/health", "/h2-console/**").permitAll();

                if (isLocalMode()) {
                    auth.requestMatchers("/api/auth/login", "/api/auth/reissue").permitAll();
                }

                auth.anyRequest().authenticated();
            })
            .addFilterBefore(apiAuthorizationFilter, AuthorizationFilter.class);

        if (isLocalMode()) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        if (isKeycloakMode()) {
            if (!keycloakEnabled) {
                throw new IllegalStateException("keycloak mode 에서는 security.keycloak.enabled=true 가 필요합니다.");
            }

            http.oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
            );
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isLocalMode() {
        return LOCAL_MODE.equalsIgnoreCase(authMode);
    }

    private boolean isKeycloakMode() {
        return KEYCLOAK_MODE.equalsIgnoreCase(authMode);
    }

    private AuthErrorCode resolveAuthenticationErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCode = oauth2Exception.getError().getErrorCode();

            if (AuthErrorCode.EXTERNAL_IDENTITY_NOT_LINKED.getCode().equals(errorCode)) {
                return AuthErrorCode.EXTERNAL_IDENTITY_NOT_LINKED;
            }

            if (AuthErrorCode.DISABLED_USER.getCode().equals(errorCode)) {
                return AuthErrorCode.DISABLED_USER;
            }

            return AuthErrorCode.INVALID_TOKEN;
        }

        return AuthErrorCode.AUTHENTICATION_REQUIRED;
    }

    private void writeErrorResponse(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponseRecord.of(errorCode));
    }
}
