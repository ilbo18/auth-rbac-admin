package com.ilbo18.authrbac.domain.auth.controller;

import com.ilbo18.authrbac.domain.auth.record.AuthRecord;
import com.ilbo18.authrbac.domain.auth.service.AuthService;
import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import com.ilbo18.authrbac.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /** 로그인 후 access token을 발급한다. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseRecord<AuthRecord.Token>> login(@Valid @RequestBody AuthRecord.Login req) {
        return ResponseEntity.ok(ApiResponseRecord.success(authService.login(req)));
    }

    /** 인증된 사용자 정보를 조회한다. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseRecord<AuthRecord.Me>> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(ApiResponseRecord.success(authService.getMe(authenticatedUser)));
    }
}
