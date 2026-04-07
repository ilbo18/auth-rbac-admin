package com.ilbo18.authrbac.domain.user.service;

import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.service.AuditService;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.mapper.UserMapper;
import com.ilbo18.authrbac.domain.user.record.UserRecord;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public void createUser(UserRecord.Create req) {
        String loginId = TextNormalizer.trimToLowerCase(req.loginId());

        validateRole(req.roleId());

        if (userRepository.existsByLoginId(loginId)) {
            throw new CustomException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(req.password());

        User user = userMapper.toEntity(req, loginId, encodedPassword);

        userRepository.save(user);
        // 감사 로그를 저장
        auditService.createAudit(AuditDomainType.USER, AuditActionType.CREATE, user.getId(), "USER 생성");
    }

    @Override
    public List<UserRecord.Response> getUsers() {
        return userRepository.findAllByDeletedFalse()
                             .stream()
                             .sorted(Comparator.comparingLong(User::getId))
                             .map(userMapper::toResponse)
                             .toList();
    }

    @Override
    public UserRecord.Response getUser(Long id) {
        User user = Optional.ofNullable(userRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserRecord.Update req) {
        User user = Optional.ofNullable(userRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        String loginId = TextNormalizer.trimToLowerCase(req.loginId());

        validateRole(req.roleId());

        if (userRepository.existsByLoginIdAndIdNot(loginId, id)) {
            throw new CustomException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(req.password());

        user.update(loginId, encodedPassword, req.name(), req.roleId(), req.enabled());
        // 감사 로그를 저장
        auditService.createAudit(AuditDomainType.USER, AuditActionType.UPDATE, user.getId(), "USER 수정");
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = Optional.ofNullable(userRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        user.delete();
        // 감사 로그를 저장
        auditService.createAudit(AuditDomainType.USER, AuditActionType.DELETE, user.getId(), "USER 삭제");
    }

    /** 역할 유효성을 검증한다. */
    private void validateRole(Long roleId) {
        Optional.ofNullable(roleRepository.findByIdAndDeletedFalse(roleId))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_ROLE));
    }
}
