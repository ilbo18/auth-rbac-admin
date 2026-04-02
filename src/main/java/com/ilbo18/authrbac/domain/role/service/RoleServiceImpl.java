package com.ilbo18.authrbac.domain.role.service;

import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.record.RoleRecord;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 역할 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void createRole(RoleRecord.Create req) {
        if (roleRepository.existsByCode(req.code())) throw new CustomException(AuthErrorCode.ROLE_ALREADY_EXISTS);

        Boolean enabled = (req.enabled() != null) ? req.enabled() : Boolean.TRUE;

        Role role = Role.builder()
                        .code(req.code())
                        .name(req.name())
                        .description(req.description())
                        .enabled(enabled)
                        .build();
        roleRepository.save(role);
    }
}
