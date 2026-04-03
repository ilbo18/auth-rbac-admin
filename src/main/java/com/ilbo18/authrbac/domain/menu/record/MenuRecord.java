package com.ilbo18.authrbac.domain.menu.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 메뉴 관련 DTO 모음
 */
public class MenuRecord {

    /** 메뉴 생성 요청 */
    public record Create(
        @NotBlank(message = "메뉴명은 필수입니다.")
        @Size(max = 100, message = "메뉴명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "메뉴 경로는 필수입니다.")
        @Size(max = 255, message = "메뉴 경로는 255자 이하여야 합니다.")
        String path,

        Long parentId,

        @NotNull(message = "정렬 순서는 필수입니다.")
        Integer sortOrder,

        Boolean enabled
    ) {}

    /** 메뉴 수정 요청 */
    public record Update(
        @NotBlank(message = "메뉴명은 필수입니다.")
        @Size(max = 100, message = "메뉴명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "메뉴 경로는 필수입니다.")
        @Size(max = 255, message = "메뉴 경로는 255자 이하여야 합니다.")
        String path,

        Long parentId,

        @NotNull(message = "정렬 순서는 필수입니다.")
        Integer sortOrder,

        @NotNull(message = "사용 여부는 필수입니다.")
        Boolean enabled
    ) {}

    /** 메뉴 응답 */
    public record Response(
        Long id,
        String name,
        String path,
        Long parentId,
        Integer sortOrder,
        Boolean enabled,
        Boolean deleted,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
}
