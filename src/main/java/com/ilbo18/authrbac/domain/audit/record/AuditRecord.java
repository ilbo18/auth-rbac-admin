package com.ilbo18.authrbac.domain.audit.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * audit 조회는 검색 조건과 페이징 응답의 성격이 분명해서
 * 전역 공용 페이징 DTO로 올리지 않고 도메인 내부 구조를 유지한다.
 */
public class AuditRecord {

    /**
     * 현재 검색 범위는 domainType, actionType, actorLoginId 수준으로 제한한다.
     * 조건 조합이 더 복잡해질 때만 조회 전략 확장
     */
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
