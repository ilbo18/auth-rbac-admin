package com.ilbo18.authrbac.domain.auth.controller;

import com.ilbo18.authrbac.domain.auth.record.AuthRecord;
import com.ilbo18.authrbac.domain.auth.service.AuthService;
import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import com.ilbo18.authrbac.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * access token과 refresh token 역할을 분리해 API 의미를 명확하게 유지한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseRecord<AuthRecord.Token>> login(@Valid @RequestBody AuthRecord.Login req) {
        return ResponseEntity.ok(ApiResponseRecord.success(authService.login(req)));
    }

    /** 토큰 재발급 */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponseRecord<AuthRecord.Token>> reissue(@Valid @RequestBody AuthRecord.Reissue req) {
        return ResponseEntity.ok(ApiResponseRecord.success(authService.reissue(req)));
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseRecord<String>> logout(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        authService.logout(authenticatedUser);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseRecord<AuthRecord.Me>> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(ApiResponseRecord.success(authService.getMe(authenticatedUser)));
    }
}
