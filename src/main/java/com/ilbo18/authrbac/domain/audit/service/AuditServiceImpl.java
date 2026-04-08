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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * 감사 로그 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final String DEFAULT_ACTOR_LOGIN_ID = "system";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

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
    public AuditRecord.PageResponse getAudits(AuditRecord.Search req) {
        int page = req.page() != null ? req.page() : DEFAULT_PAGE;
        int size = req.size() != null ? req.size() : DEFAULT_SIZE;
        String actorLoginId = StringUtils.hasText(req.actorLoginId()) ? req.actorLoginId().trim() : null;

        Audit probe = Audit.builder()
                           .actorLoginId(actorLoginId)
                           .domainType(toDomainType(req.domainType()))
                           .actionType(toActionType(req.actionType()))
                           .build();

        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                                               .withIgnoreNullValues();

        if (StringUtils.hasText(actorLoginId)) {
            matcher = matcher.withMatcher("actorLoginId", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());
        }

        Page<Audit> auditPage = auditRepository.findAll(
            Example.of(probe, matcher),
            PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );

        return new AuditRecord.PageResponse(
            auditPage.getContent().stream().map(auditMapper::toResponse).toList(),
            auditPage.getNumber(),
            auditPage.getSize(),
            auditPage.getTotalElements(),
            auditPage.getTotalPages(),
            auditPage.isFirst(),
            auditPage.isLast()
        );
    }

    @Override
    public AuditRecord.Response getAudit(Long id) {
        Audit audit = Optional.ofNullable(auditRepository.findOneById(id))
                              .orElseThrow(() -> new CustomException(AuthErrorCode.AUDIT_NOT_FOUND));

        return auditMapper.toResponse(audit);
    }

    /** 문자열 검색 조건을 AuditDomainType으로 변환한다. */
    private AuditDomainType toDomainType(String domainType) {
        if (!StringUtils.hasText(domainType)) {
            return null;
        }

        try {
            return AuditDomainType.valueOf(domainType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.BAD_REQUEST);
        }
    }

    /** 문자열 검색 조건을 AuditActionType으로 변환한다. */
    private AuditActionType toActionType(String actionType) {
        if (!StringUtils.hasText(actionType)) {
            return null;
        }

        try {
            return AuditActionType.valueOf(actionType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.BAD_REQUEST);
        }
    }
}
