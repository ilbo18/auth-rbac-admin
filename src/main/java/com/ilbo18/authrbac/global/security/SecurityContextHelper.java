package com.ilbo18.authrbac.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityContext 기반 현재 인증 사용자 조회 helper
 */
@Component
public class SecurityContextHelper {

    /** 현재 인증 사용자를 반환하고 없으면 null을 반환한다. */
    public AuthenticatedUser getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }

        return authenticatedUser;
    }
}
