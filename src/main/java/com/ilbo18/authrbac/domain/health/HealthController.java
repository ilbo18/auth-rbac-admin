package com.ilbo18.authrbac.domain.health;

import java.util.Map;

import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 상태를 확인하는 헬스 체크 API 컨트롤러
 */
@RestController
public class HealthController {

    /**
     * 애플리케이션 기본 상태 정보를 반환한다.
     */
    @GetMapping("/api/health")
    public ApiResponseRecord<Map<String, String>> health() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "auth-rbac-admin"
        );

        return ApiResponseRecord.success(response);
    }
}
