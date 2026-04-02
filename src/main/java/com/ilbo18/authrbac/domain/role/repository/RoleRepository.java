package com.ilbo18.authrbac.domain.role.repository;

import com.ilbo18.authrbac.domain.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 역할 데이터 접근 레포지토리
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByCode(String code);

    List<Role> findAllByDeletedFalse();

    Role findByIdAndDeletedFalse(Long id);

}
