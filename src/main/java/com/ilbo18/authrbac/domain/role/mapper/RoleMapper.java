package com.ilbo18.authrbac.domain.role.mapper;

import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.record.RoleRecord;
import org.springframework.stereotype.Component;

/**
 * Role 엔티티와 DTO 간 변환을 담당하는 매퍼
 */
@Component
public class RoleMapper {

    /** 역할 생성 요청 DTO를 Role Entity로 변환 */
    public Role toEntity(RoleRecord.Create req) {
        Boolean enabled = (req.enabled() != null) ? req.enabled() : Boolean.TRUE;

        return Role.builder()
                   .code(req.code())
                   .name(req.name())
                   .description(req.description())
                   .enabled(enabled)
                   .build();
    }

    /** Role Entity를 역할 응답 DTO로 변환 */
    public RoleRecord.Response toResponse(Role role) {
        return new RoleRecord.Response(
            role.getId(),
            role.getCode(),
            role.getName(),
            role.getDescription(),
            role.getEnabled(),
            role.getDeleted(),
            role.getCreatedBy(),
            role.getUpdatedBy(),
            role.getCreatedAt(),
            role.getUpdatedAt()
        );
    }
}
