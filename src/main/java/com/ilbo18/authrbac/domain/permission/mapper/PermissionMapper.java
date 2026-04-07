package com.ilbo18.authrbac.domain.permission.mapper;

import com.ilbo18.authrbac.domain.permission.entity.Permission;
import com.ilbo18.authrbac.domain.permission.record.PermissionRecord;
import org.springframework.stereotype.Component;

/**
 * Permission 엔티티와 DTO 간 변환을 담당하는 매퍼
 */
@Component
public class PermissionMapper {

    /** 권한 생성 요청 DTO를 Permission Entity로 변환 */
    public Permission toEntity(PermissionRecord.Create req) {
        Boolean enabled = (req.enabled() != null) ? req.enabled() : Boolean.TRUE;

        return Permission.builder()
                         .roleId(req.roleId())
                         .menuId(req.menuId())
                         .canRead(req.canRead())
                         .canCreate(req.canCreate())
                         .canUpdate(req.canUpdate())
                         .canDelete(req.canDelete())
                         .enabled(enabled)
                         .build();
    }

    /** Permission Entity를 권한 응답 DTO로 변환 */
    public PermissionRecord.Response toResponse(Permission permission) {
        return new PermissionRecord.Response(
            permission.getId(),
            permission.getRoleId(),
            permission.getMenuId(),
            permission.getCanRead(),
            permission.getCanCreate(),
            permission.getCanUpdate(),
            permission.getCanDelete(),
            permission.getEnabled(),
            permission.getDeleted(),
            permission.getCreatedBy(),
            permission.getUpdatedBy(),
            permission.getCreatedAt(),
            permission.getUpdatedAt()
        );
    }
}
