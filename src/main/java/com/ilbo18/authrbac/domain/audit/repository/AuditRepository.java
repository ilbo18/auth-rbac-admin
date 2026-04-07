package com.ilbo18.authrbac.domain.audit.repository;

import com.ilbo18.authrbac.domain.audit.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 감사 로그 데이터 접근 리포지토리
 */
public interface AuditRepository extends JpaRepository<Audit, Long> {

    Audit findOneById(Long id);
}
