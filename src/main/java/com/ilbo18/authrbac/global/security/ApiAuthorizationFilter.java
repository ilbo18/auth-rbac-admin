package com.ilbo18.authrbac.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.exception.ErrorCode;
import com.ilbo18.authrbac.global.response.ErrorResponseRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * local JWT 든 Keycloak JWT 든 최종 principal 이 같으면
 * RBAC 검사는 하나의 필터로 유지하는 편이 구조가 덜 흔들린다.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthorizationFilter extends OncePerRequestFilter {

    private final ApiAuthorizationRule apiAuthorizationRule;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!apiAuthorizationRule.requiresAuthorization(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        AuthenticatedUser authenticatedUser = principal instanceof AuthenticatedUser ? (AuthenticatedUser) principal : null;

        try {
            apiAuthorizationRule.validate(authenticatedUser, request);
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, e.getErrorCode());
        }
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponseRecord.of(errorCode));
    }
}
