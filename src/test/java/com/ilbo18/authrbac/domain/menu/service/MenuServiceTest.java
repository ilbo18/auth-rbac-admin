package com.ilbo18.authrbac.domain.menu.service;

import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 메뉴 서비스의 기본 CRUD 흐름을 검증한다.
 */
@SpringBootTest
@Transactional
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @Test
    void 메뉴를_생성하고_조회할_수_있다() {
        // given
        MenuRecord.Create req = new MenuRecord.Create("Dashboard", "/dashboard", null, 1, true);

        // when
        menuService.createMenu(req);
        MenuRecord.Response savedMenu = findMenuByPath("/dashboard");
        MenuRecord.Response foundMenu = menuService.getMenu(savedMenu.id());

        // then
        assertThat(savedMenu.path()).isEqualTo("/dashboard");
        assertThat(foundMenu.name()).isEqualTo("Dashboard");
        assertThat(foundMenu.enabled()).isTrue();
    }

    @Test
    void 메뉴_경로가_중복되면_생성에_실패한다() {
        // given
        menuService.createMenu(new MenuRecord.Create("Dashboard", "/dashboard", null, 1, true));
        MenuRecord.Create req = new MenuRecord.Create("Dashboard2", "/dashboard", null, 2, true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> menuService.createMenu(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.MENU_ALREADY_EXISTS);
    }

    @Test
    void 메뉴를_삭제하면_조회에_실패한다() {
        // given
        menuService.createMenu(new MenuRecord.Create("Users", "/users", null, 1, true));
        Long menuId = findMenuByPath("/users").id();

        // when
        menuService.deleteMenu(menuId);
        CustomException exception = assertThrows(CustomException.class, () -> menuService.getMenu(menuId));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.MENU_NOT_FOUND);
    }

    /** 경로 기준으로 메뉴를 찾아 테스트 대상을 명확히 식별한다. */
    private MenuRecord.Response findMenuByPath(String path) {
        return menuService.getMenus()
                          .stream()
                          .filter(menu -> path.equals(menu.path()))
                          .findFirst()
                          .orElseThrow();
    }
}
