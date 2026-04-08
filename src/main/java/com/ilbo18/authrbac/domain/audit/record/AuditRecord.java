package com.ilbo18.authrbac.domain.audit.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 로그 관련 DTO 모음
 */
public class AuditRecord {

    /** 감사 로그 검색 조건 */
    public record Search(
        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        @Size(max = 50, message = "domainType은 50자 이하여야 합니다.")
        String domainType,

        @Size(max = 50, message = "actionType은 50자 이하여야 합니다.")
        String actionType,

        @Size(max = 100, message = "actorLoginId는 100자 이하여야 합니다.")
        String actorLoginId
    ) {}

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

    /** 감사 로그 페이지 응답 */
    public record PageResponse(
        List<Response> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
    ) {}
}
