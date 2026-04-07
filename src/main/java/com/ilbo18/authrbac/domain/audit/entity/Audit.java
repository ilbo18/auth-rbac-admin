package com.ilbo18.authrbac.domain.audit.entity;

import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 작업 감사 로그 엔티티
 */
@Getter
@Entity
@Table(name = "audits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작업 수행 사용자 ID */
    @Column
    private Long actorUserId;

    /** 작업 수행 로그인 ID */
    @Column(nullable = false, length = 100)
    private String actorLoginId;

    /** 작업 대상 도메인 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditDomainType domainType;

    /** 작업 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditActionType actionType;

    /** 작업 대상 ID */
    @Column
    private Long targetId;

    /** 작업 설명 */
    @Column(nullable = false, length = 255)
    private String description;

    /** 로그 생성 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 생성 시각을 초기화한다. */
    @PrePersist
    public void initCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
