package com.ilbo18.authrbac.domain.menu.repository;

import com.ilbo18.authrbac.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 메뉴 데이터 접근 리포지토리
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

    boolean existsByRoutePath(String routePath);

    boolean existsByRoutePathAndIdNot(String routePath, Long id);

    boolean existsByApiPath(String apiPath);

    boolean existsByApiPathAndIdNot(String apiPath, Long id);

    List<Menu> findAllByDeletedFalseAndEnabledTrue();

    List<Menu> findAllByDeletedFalse();

    Menu findByIdAndDeletedFalse(Long id);
}
