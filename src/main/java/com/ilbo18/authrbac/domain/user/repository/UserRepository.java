package com.ilbo18.authrbac.domain.user.repository;

import com.ilbo18.authrbac.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자 데이터 접근 리포지토리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByLoginIdAndIdNot(String loginId, Long id);

    List<User> findAllByDeletedFalse();

    User findByIdAndDeletedFalse(Long id);

    User findByLoginIdAndDeletedFalse(String loginId);
}
