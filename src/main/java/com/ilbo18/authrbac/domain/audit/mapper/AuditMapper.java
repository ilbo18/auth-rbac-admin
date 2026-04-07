package com.ilbo18.authrbac.domain.audit.mapper;

import com.ilbo18.authrbac.domain.audit.entity.Audit;
import com.ilbo18.authrbac.domain.audit.record.AuditRecord;
import org.springframework.stereotype.Component;

/**
 * Audit 엔티티와 DTO 간 변환을 담당하는 매퍼
 */
@Component
public class AuditMapper {


    /** Audit 엔티티를 응답 DTO로 변환한다. */
    public AuditRecord.Response toResponse(Audit audit) {
        return new AuditRecord.Response(
            audit.getId(),
            audit.getActorUserId(),
            audit.getActorLoginId(),
            audit.getDomainType().name(),
            audit.getActionType().name(),
            audit.getTargetId(),
            audit.getDescription(),
            audit.getCreatedAt()
        );
    }
}
