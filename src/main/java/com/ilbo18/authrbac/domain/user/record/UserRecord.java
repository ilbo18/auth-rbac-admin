package com.ilbo18.authrbac.domain.user.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 사용자 관련 DTO 모음
 */
public class UserRecord {

    /** 사용자 생성 요청 */
    public record Create(
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 20, message = "로그인 ID는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,20}$",
                message = "로그인 ID는 4자 이상 20자 이하의 영문 소문자와 숫자만 사용할 수 있습니다."
        )
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 20, message = "비밀번호는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-])[A-Za-z\\d!@#$%^&*()_+\\-]{8,20}$",
                message = "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(max = 10, message = "사용자명은 10자 이하여야 합니다.")
        String name,

        @NotNull(message = "역할 ID는 필수입니다.")
        Long roleId,

        Boolean enabled
    ) {}

    /** 사용자 수정 요청 */
    public record Update(
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 20, message = "로그인 ID는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,20}$",
                message = "로그인 ID는 4자 이상 20자 이하의 영문 소문자와 숫자만 사용할 수 있습니다."
        )
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 20, message = "비밀번호는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-])[A-Za-z\\d!@#$%^&*()_+\\-]{8,20}$",
                message = "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(max = 10, message = "사용자명은 10자 이하여야 합니다.")
        String name,

        @NotNull(message = "역할 ID는 필수입니다.")
        Long roleId,

        @NotNull(message = "사용 여부는 필수입니다.")
        Boolean enabled
    ) {}

    /** 사용자 응답 */
    public record Response(
        Long id,
        String loginId,
        String name,
        Long roleId,
        Boolean enabled,
        Boolean deleted,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
}
