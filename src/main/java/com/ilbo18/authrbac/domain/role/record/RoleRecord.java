package com.ilbo18.authrbac.domain.role.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 역할 도메인 DTO 모음
 */
public class RoleRecord {

    /** 역할 생성 요청 */
    public record Create(
        @NotBlank(message = "역할 코드는 필수입니다.")
        @Size(max = 50, message = "역할 코드는 50자 이하여야 합니다.")
        String code,

        @NotBlank(message = "역할명은 필수입니다.")
        @Size(max = 100, message = "역할명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 255, message = "설명은 255자 이하여야 합니다.")
        String description,

        Boolean enabled
    ) {}

    /** 역할 수정 요청 */
    public record Update(
        @NotBlank(message = "역할명은 필수입니다.")
        @Size(max = 100, message = "역할명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 255, message = "설명은 255자 이하여야 합니다.")
        String description,

        Boolean enabled
    ) {}

    /** 역할 응답 */
    public record Response(
        Long id,
        String code,
        String name,
        String description,
        Boolean enabled,
        Boolean deleted,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
}
