package com.ilbo18.authrbac.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing 기능 활성화 설정
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /** 생성자, 수정자에 들어갈 현재 사용자 정보 */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("SYSTEM");
    }
}
