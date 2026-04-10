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
import com.ilbo18.authrbac.global.util.EnumParser;
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
 * 현재 audit 검색 축은 단순해서 ExampleMatcher 기반 조회가 가장 짧고 읽기 쉽다.
 * 기간 검색이나 조합 조건이 늘어나는 시점에만 QueryDSL 또는 JPQL을 검토한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final String DEFAULT_ACTOR_LOGIN_ID = "system";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final Sort DEFAULT_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

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
                           .domainType(EnumParser.parseOrNull(req.domainType(), AuditDomainType.class, AuthErrorCode.BAD_REQUEST))
                           .actionType(EnumParser.parseOrNull(req.actionType(), AuditActionType.class, AuthErrorCode.BAD_REQUEST))
                           .build();

        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                                               .withIgnoreNullValues();

        if (StringUtils.hasText(actorLoginId)) {
            matcher = matcher.withMatcher("actorLoginId", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());
        }

        Page<Audit> auditPage = auditRepository.findAll(Example.of(probe, matcher), PageRequest.of(page, size, DEFAULT_SORT));

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
}
