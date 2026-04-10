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
import java.util.Locale;
import java.util.Set;

/**
 * JWT 필터는 인증에 집중하고, API 인가 판단은 이 컴포넌트에서 맡는다.
 * 현재는 menu.apiPath 와 HTTP method 만으로 규칙을 설명할 수 있어
 * 복잡한 인가 프레임워크 대신 이 수준을 유지한다.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthorizationRule {

    private static final String API_PREFIX = "/api/";
    private static final String H2_CONSOLE_PATH = "/h2-console";

    private static final Set<String> EXCLUDED_API_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/me",
        "/api/health"
    );

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;

    public boolean requiresAuthorization(HttpServletRequest request) {
        String requestUri = normalizePath(request.getRequestURI());

        return isApiRequest(requestUri)
            && !isPreflight(request.getMethod())
            && !isExcludedPath(requestUri);
    }

    public void validate(AuthenticatedUser authenticatedUser, HttpServletRequest request) {
        if (authenticatedUser == null) {
            throw new CustomException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        String requestUri = normalizePath(request.getRequestURI());
        Menu menu = resolveProtectedMenu(requestUri);

        if (menu == null) {
            throw new CustomException(AuthErrorCode.FORBIDDEN);
        }

        Permission permission = permissionRepository.findByRoleIdAndMenuIdAndDeletedFalseAndEnabledTrue(
            authenticatedUser.roleId(),
            menu.getId()
        );

        if (!isAllowed(request.getMethod(), permission)) {
            throw new CustomException(AuthErrorCode.FORBIDDEN);
        }
    }

    private boolean isApiRequest(String requestUri) {
        return requestUri.startsWith(API_PREFIX);
    }

    private boolean isPreflight(String requestMethod) {
        return "OPTIONS".equalsIgnoreCase(requestMethod);
    }

    private boolean isExcludedPath(String requestUri) {
        return EXCLUDED_API_PATHS.contains(requestUri) || requestUri.startsWith(H2_CONSOLE_PATH);
    }

    private Menu resolveProtectedMenu(String requestUri) {
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
        if (permission == null) {
            return false;
        }

        return switch (requestMethod.toUpperCase(Locale.ROOT)) {
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
