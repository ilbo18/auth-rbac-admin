package com.ilbo18.authrbac.domain.role.service;

import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.service.AuditService;
import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.mapper.RoleMapper;
import com.ilbo18.authrbac.domain.role.record.RoleRecord;
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
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public void createRole(RoleRecord.Create req) {
        if (roleRepository.existsByCode(req.code())) {
            throw new CustomException(AuthErrorCode.ROLE_ALREADY_EXISTS);
        }

        Role role = roleMapper.toEntity(req);

        roleRepository.save(role);
        auditService.createAudit(AuditDomainType.ROLE, AuditActionType.CREATE, role.getId(), "ROLE 생성");
    }

    @Override
    public List<RoleRecord.Response> getRoles() {
        return roleRepository.findAllByDeletedFalse()
                             .stream()
                             .sorted(Comparator.comparingLong(Role::getId))
                             .map(roleMapper::toResponse)
                             .toList();
    }

    @Override
    public RoleRecord.Response getRole(Long id) {
        Role role = Optional.ofNullable(roleRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.ROLE_NOT_FOUND));

        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public void updateRole(Long id, RoleRecord.Update req) {
        Role role = Optional.ofNullable(roleRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.ROLE_NOT_FOUND));

        role.update(req.name(), req.description(), req.enabled());
        auditService.createAudit(AuditDomainType.ROLE, AuditActionType.UPDATE, role.getId(), "ROLE 수정");
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = Optional.ofNullable(roleRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.ROLE_NOT_FOUND));

        role.delete();
        auditService.createAudit(AuditDomainType.ROLE, AuditActionType.DELETE, role.getId(), "ROLE 삭제");
    }
}
