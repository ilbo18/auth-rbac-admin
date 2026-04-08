package com.ilbo18.authrbac.domain.permission.repository;

import com.ilbo18.authrbac.domain.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 권한 데이터 접근 리포지토리
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByRoleIdAndMenuIdAndDeletedFalse(Long roleId, Long menuId);

    boolean existsByRoleIdAndMenuIdAndDeletedFalseAndIdNot(Long roleId, Long menuId, Long id);

    List<Permission> findAllByDeletedFalse();

    Permission findByRoleIdAndMenuIdAndDeletedFalseAndEnabledTrue(Long roleId, Long menuId);

    Permission findByIdAndDeletedFalse(Long id);
}
