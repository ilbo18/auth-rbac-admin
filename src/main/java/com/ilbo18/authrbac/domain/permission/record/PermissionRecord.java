package com.ilbo18.authrbac.domain.permission.record;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 권한 관련 DTO 모음
 */
public class PermissionRecord {

    /** 권한 생성 요청 */
    public record Create(
        @NotNull(message = "역할 ID는 필수입니다.")
        Long roleId,

        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @NotNull(message = "조회 권한은 필수입니다.")
        Boolean canRead,

        @NotNull(message = "생성 권한은 필수입니다.")
        Boolean canCreate,

        @NotNull(message = "수정 권한은 필수입니다.")
        Boolean canUpdate,

        @NotNull(message = "삭제 권한은 필수입니다.")
        Boolean canDelete,

        Boolean enabled
    ) {}

    /** 권한 수정 요청 */
    public record Update(
        @NotNull(message = "역할 ID는 필수입니다.")
        Long roleId,

        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @NotNull(message = "조회 권한은 필수입니다.")
        Boolean canRead,

        @NotNull(message = "생성 권한은 필수입니다.")
        Boolean canCreate,

        @NotNull(message = "수정 권한은 필수입니다.")
        Boolean canUpdate,

        @NotNull(message = "삭제 권한은 필수입니다.")
        Boolean canDelete,

        @NotNull(message = "사용 여부는 필수입니다.")
        Boolean enabled
    ) {}

    /** 권한 응답 */
    public record Response(
        Long id,
        Long roleId,
        Long menuId,
        Boolean canRead,
        Boolean canCreate,
        Boolean canUpdate,
        Boolean canDelete,
        Boolean enabled,
        Boolean deleted,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
}
