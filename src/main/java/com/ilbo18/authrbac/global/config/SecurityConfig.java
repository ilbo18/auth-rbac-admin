package com.ilbo18.authrbac.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 기본 보안 정책 설정
 */
@Configuration
public class SecurityConfig {

    /** 요청별 접근 정책과 기본 보안 옵션을 설정 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // POST/PUT/DELETE 요청 시 사용하는 CSRF 보호 비활성화
            .csrf(csrf -> csrf.disable())
            // H2 콘솔이 iframe 기반으로 동작할 수 있도록 same-origin만 허용
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // 상태 확인, 로컬 DB 콘솔은 인증 없이 허용하고 나머지는 인증 필요
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            // 가장 단순한 기본 인증 방식 활성화
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
