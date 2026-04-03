package com.ilbo18.authrbac.domain.menu.repository;

import com.ilbo18.authrbac.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 메뉴 데이터 접근 리포지토리
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

    boolean existsByPath(String path);

    boolean existsByPathAndIdNot(String path, Long id);

    List<Menu> findAllByDeletedFalse();

    Menu findByIdAndDeletedFalse(Long id);
}
