package com.ilbo18.authrbac.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 공통 메타 정보를 관리하는 부모 엔티티
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** 삭제 여부 */
    @Column(nullable = false)
    protected Boolean deleted = false;

    /** 생성 일시 */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 생성자 */
    @CreatedBy
    @Column(nullable = false, updatable = false, length = 50)
    private String createdBy;

    /** 수정자 */
    @LastModifiedBy
    @Column(nullable = false, length = 50)
    private String updatedBy;

    /** Entity 삭제 */
    public void delete() {
        this.deleted = true;
    }
}
