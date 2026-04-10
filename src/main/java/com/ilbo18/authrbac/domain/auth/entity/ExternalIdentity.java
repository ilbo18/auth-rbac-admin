package com.ilbo18.authrbac.domain.auth.entity;

import com.ilbo18.authrbac.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 IdP 사용자와 내부 User 를 명시적으로 연결하는 엔티티
 */
@Getter
@Entity
@Table(
    name = "external_identities",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ExternalIdentity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 내부 사용자 ID */
    @Column(nullable = false)
    private Long userId;

    /**
     * provider 값을 남겨두면 현재는 KEYCLOAK 하나만 써도
     * 이후 다른 사내 IdP 를 붙일 때 테이블 구조를 다시 바꾸지 않아도 된다.
     */
    @Column(nullable = false, length = 30)
    private String provider;

    /** 외부 IdP 의 고유 사용자 식별자 */
    @Column(name = "provider_user_id", nullable = false, length = 150)
    private String providerUserId;

    /** 연결 사용 여부 */
    @Column(nullable = false)
    private Boolean enabled;
}
