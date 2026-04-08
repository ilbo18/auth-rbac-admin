package com.ilbo18.authrbac.domain.audit.service;

import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.record.AuditRecord;

public interface AuditService {

    /** 감사 로그를 저장한다. */
    void createAudit(AuditDomainType domainType, AuditActionType actionType, Long targetId, String description);

    /** 감사 로그 목록을 조회한다. */
    AuditRecord.PageResponse getAudits(AuditRecord.Search req);

    /** 감사 로그 단건을 조회한다. */
    AuditRecord.Response getAudit(Long id);
}
