package com.ilbo18.authrbac.domain.audit.service;

import com.ilbo18.authrbac.domain.audit.entity.Audit;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.mapper.AuditMapper;
import com.ilbo18.authrbac.domain.audit.record.AuditRecord;
import com.ilbo18.authrbac.domain.audit.repository.AuditRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.security.AuthenticatedUser;
import com.ilbo18.authrbac.global.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 감사 로그 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final String DEFAULT_ACTOR_LOGIN_ID = "system";

    private final AuditRepository auditRepository;
    private final AuditMapper auditMapper;
    private final SecurityContextHelper securityContextHelper;

    @Override
    @Transactional
    public void createAudit(AuditDomainType domainType, AuditActionType actionType, Long targetId, String description) {
        AuthenticatedUser authenticatedUser = securityContextHelper.getCurrentAuthenticatedUser();

        Audit audit = Audit.builder()
                           .actorUserId(authenticatedUser != null ? authenticatedUser.userId() : null)
                           .actorLoginId(authenticatedUser != null ? authenticatedUser.loginId() : DEFAULT_ACTOR_LOGIN_ID)
                           .domainType(domainType)
                           .actionType(actionType)
                           .targetId(targetId)
                           .description(description)
                           .build();

        auditRepository.save(audit);
    }

    @Override
    public List<AuditRecord.Response> getAudits() {
        return auditRepository.findAll()
                              .stream()
                              .sorted(Comparator.comparing(Audit::getCreatedAt).reversed().thenComparing(Audit::getId, Comparator.reverseOrder()))
                              .map(auditMapper::toResponse)
                              .toList();
    }

    @Override
    public AuditRecord.Response getAudit(Long id) {
        Audit audit = Optional.ofNullable(auditRepository.findOneById(id))
                              .orElseThrow(() -> new CustomException(AuthErrorCode.AUDIT_NOT_FOUND));

        return auditMapper.toResponse(audit);
    }
}
