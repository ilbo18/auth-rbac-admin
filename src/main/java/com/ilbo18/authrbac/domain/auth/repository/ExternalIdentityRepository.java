package com.ilbo18.authrbac.domain.auth.repository;

import com.ilbo18.authrbac.domain.auth.entity.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 외부 인증 사용자 매핑 조회 리포지토리
 */
public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {

    ExternalIdentity findByProviderAndProviderUserIdAndDeletedFalseAndEnabledTrue(String provider, String providerUserId);
}
