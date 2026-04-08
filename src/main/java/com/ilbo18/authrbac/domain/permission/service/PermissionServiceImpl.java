package com.ilbo18.authrbac.domain.permission.service;

import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.service.AuditService;
import com.ilbo18.authrbac.domain.menu.repository.MenuRepository;
import com.ilbo18.authrbac.domain.permission.entity.Permission;
import com.ilbo18.authrbac.domain.permission.mapper.PermissionMapper;
import com.ilbo18.authrbac.domain.permission.record.PermissionRecord;
import com.ilbo18.authrbac.domain.permission.repository.PermissionRepository;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final PermissionMapper permissionMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public void createPermission(PermissionRecord.Create req) {
        validateRole(req.roleId());
        validateMenu(req.menuId());
        validateActions(req.canRead(), req.canCreate(), req.canUpdate(), req.canDelete());

        if (permissionRepository.existsByRoleIdAndMenuIdAndDeletedFalse(req.roleId(), req.menuId())) {
            throw new CustomException(AuthErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        Permission permission = permissionMapper.toEntity(req);

        permissionRepository.save(permission);
        auditService.createAudit(AuditDomainType.PERMISSION, AuditActionType.CREATE, permission.getId(), "PERMISSION 생성");
    }

    @Override
    public List<PermissionRecord.Response> getPermissions() {
        return permissionRepository.findAllByDeletedFalse()
                                   .stream()
                                   .sorted(Comparator.comparingLong(Permission::getId))
                                   .map(permissionMapper::toResponse)
                                   .toList();
    }

    @Override
    public PermissionRecord.Response getPermission(Long id) {
        Permission permission = Optional.ofNullable(permissionRepository.findByIdAndDeletedFalse(id))
                                        .orElseThrow(() -> new CustomException(AuthErrorCode.PERMISSION_NOT_FOUND));

        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional
    public void updatePermission(Long id, PermissionRecord.Update req) {
        Permission permission = Optional.ofNullable(permissionRepository.findByIdAndDeletedFalse(id))
                                        .orElseThrow(() -> new CustomException(AuthErrorCode.PERMISSION_NOT_FOUND));

        validateRole(req.roleId());
        validateMenu(req.menuId());
        validateActions(req.canRead(), req.canCreate(), req.canUpdate(), req.canDelete());

        if (permissionRepository.existsByRoleIdAndMenuIdAndDeletedFalseAndIdNot(req.roleId(), req.menuId(), id)) {
            throw new CustomException(AuthErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        permission.update(
            req.roleId(),
            req.menuId(),
            req.canRead(),
            req.canCreate(),
            req.canUpdate(),
            req.canDelete(),
            req.enabled()
        );
        auditService.createAudit(AuditDomainType.PERMISSION, AuditActionType.UPDATE, permission.getId(), "PERMISSION 수정");
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = Optional.ofNullable(permissionRepository.findByIdAndDeletedFalse(id))
                                        .orElseThrow(() -> new CustomException(AuthErrorCode.PERMISSION_NOT_FOUND));

        permission.delete();
        auditService.createAudit(AuditDomainType.PERMISSION, AuditActionType.DELETE, permission.getId(), "PERMISSION 삭제");
    }

    private void validateRole(Long roleId) {
        Optional.ofNullable(roleRepository.findByIdAndDeletedFalse(roleId))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_ROLE));
    }

    private void validateMenu(Long menuId) {
        Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(menuId))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_MENU));
    }

    private void validateActions(Boolean canRead, Boolean canCreate, Boolean canUpdate, Boolean canDelete) {
        if (!Boolean.TRUE.equals(canRead)
                && !Boolean.TRUE.equals(canCreate)
                && !Boolean.TRUE.equals(canUpdate)
                && !Boolean.TRUE.equals(canDelete)) {
            throw new CustomException(AuthErrorCode.INVALID_PERMISSION_ACTION);
        }
    }
}
