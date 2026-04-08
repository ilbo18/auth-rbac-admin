package com.ilbo18.authrbac.global.security;

import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.repository.MenuRepository;
import com.ilbo18.authrbac.domain.permission.entity.Permission;
import com.ilbo18.authrbac.domain.permission.repository.PermissionRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * 필터가 토큰 처리에 집중할 수 있도록 API 인가 규칙을 분리한다.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthorizationRule {

    private static final String API_PREFIX = "/api/";
    private static final String AUTH_LOGIN_PATH = "/api/auth/login";
    private static final String AUTH_ME_PATH = "/api/auth/me";
    private static final String HEALTH_PATH = "/api/health";
    private static final String H2_CONSOLE_PATH = "/h2-console";

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;

    public boolean requiresAuthorization(HttpServletRequest request) {
        String requestUri = normalizePath(request.getRequestURI());

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        if (!requestUri.startsWith(API_PREFIX)) {
            return false;
        }

        return !isExcludedPath(requestUri);
    }

    public void validate(AuthenticatedUser authenticatedUser, HttpServletRequest request) {
        String requestUri = normalizePath(request.getRequestURI());
        Menu menu = findMatchedMenu(requestUri);

        if (menu == null) {
            throw new CustomException(AuthErrorCode.FORBIDDEN);
        }

        Permission permission = permissionRepository.findByRoleIdAndMenuIdAndDeletedFalseAndEnabledTrue(
            authenticatedUser.roleId(),
            menu.getId()
        );

        if (permission == null || !isAllowed(request.getMethod(), permission)) {
            throw new CustomException(AuthErrorCode.FORBIDDEN);
        }
    }

    private boolean isExcludedPath(String requestUri) {
        return AUTH_LOGIN_PATH.equals(requestUri)
            || AUTH_ME_PATH.equals(requestUri)
            || HEALTH_PATH.equals(requestUri)
            || requestUri.startsWith(H2_CONSOLE_PATH);
    }

    private Menu findMatchedMenu(String requestUri) {
        return menuRepository.findAllByDeletedFalseAndEnabledTrue()
                             .stream()
                             .filter(menu -> matchesApiPath(requestUri, menu.getApiPath()))
                             .max(Comparator.comparingInt(menu -> normalizePath(menu.getApiPath()).length()))
                             .orElse(null);
    }

    private boolean matchesApiPath(String requestUri, String apiPath) {
        String normalizedApiPath = normalizePath(apiPath);

        return requestUri.equals(normalizedApiPath) || requestUri.startsWith(normalizedApiPath + "/");
    }

    private boolean isAllowed(String requestMethod, Permission permission) {
        return switch (requestMethod.toUpperCase()) {
            case "GET", "HEAD" -> Boolean.TRUE.equals(permission.getCanRead());
            case "POST" -> Boolean.TRUE.equals(permission.getCanCreate());
            case "PUT", "PATCH" -> Boolean.TRUE.equals(permission.getCanUpdate());
            case "DELETE" -> Boolean.TRUE.equals(permission.getCanDelete());
            default -> false;
        };
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }
}
