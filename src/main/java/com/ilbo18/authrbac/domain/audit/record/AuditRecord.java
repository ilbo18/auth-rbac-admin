package com.ilbo18.authrbac.domain.audit.record;

import java.time.LocalDateTime;

/**
 * 감사 로그 관련 DTO 모음
 */
public class AuditRecord {

    /** 감사 로그 응답 */
    public record Response(
        Long id,
        Long actorUserId,
        String actorLoginId,
        String domainType,
        String actionType,
        Long targetId,
        String description,
        LocalDateTime createdAt
    ) {}
}
