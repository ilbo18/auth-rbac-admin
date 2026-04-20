package com.ilbo18.authrbac.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.exception.ErrorCode;
import com.ilbo18.authrbac.global.response.ErrorResponseRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

/**
 * local mode 에서만 앱이 직접 발급한 JWT 를 처리한다.
 * keycloak mode 에서는 이 필터가 인증을 맡지 않고 resource server 쪽으로 흐름을 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LOCAL_MODE = "local";

    private static final Set<String> SKIP_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/reissue"
    );

    @Value("${auth.mode:local}")
    private String authMode;

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SKIP_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!LOCAL_MODE.equalsIgnoreCase(authMode)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);

            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            writeErrorResponse(response, AuthErrorCode.INVALID_TOKEN);

            return;
        }

        if (!jwtTokenProvider.isLocalToken(token)) {
            writeErrorResponse(response, AuthErrorCode.INVALID_TOKEN);
            return;
        }

        try {
            AuthenticatedUser authenticatedUser = jwtTokenProvider.getAuthenticatedUser(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, e.getErrorCode());
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponseRecord.of(errorCode));
    }
}
