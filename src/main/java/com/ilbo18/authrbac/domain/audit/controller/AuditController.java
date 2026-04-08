package com.ilbo18.authrbac.domain.audit.controller;

import com.ilbo18.authrbac.domain.audit.record.AuditRecord;
import com.ilbo18.authrbac.domain.audit.service.AuditService;
import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 감사 로그 조회 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audits")
public class AuditController {

    private final AuditService auditService;

    /** 감사 로그 목록을 조회한다. */
    @GetMapping
    public ResponseEntity<ApiResponseRecord<AuditRecord.PageResponse>> getAudits(@Valid @ModelAttribute AuditRecord.Search req) {
        return ResponseEntity.ok(ApiResponseRecord.success(auditService.getAudits(req)));
    }

    /** 감사 로그 단건을 조회한다. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<AuditRecord.Response>> getAudit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseRecord.success(auditService.getAudit(id)));
    }
}
