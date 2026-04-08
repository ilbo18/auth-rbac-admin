package com.ilbo18.authrbac.domain.menu.service;

import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 메뉴 CRUD와 트리 응답을 검증한다.
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @Test
    void 메뉴를_생성하고_조회할_수_있다() {
        // given
        MenuRecord.Create req = new MenuRecord.Create("Dashboard", "/admin/dashboard", "/api/dashboard", null, 1, true);

        // when
        menuService.createMenu(req);
        MenuRecord.Response savedMenu = findMenuByRoutePath("/admin/dashboard");
        MenuRecord.Response foundMenu = menuService.getMenu(savedMenu.id());

        // then
        assertThat(savedMenu.routePath()).isEqualTo("/admin/dashboard");
        assertThat(savedMenu.apiPath()).isEqualTo("/api/dashboard");
        assertThat(foundMenu.name()).isEqualTo("Dashboard");
        assertThat(foundMenu.enabled()).isTrue();
    }

    @Test
    void 메뉴_이동경로가_중복되면_생성에_실패한다() {
        // given
        menuService.createMenu(new MenuRecord.Create("Dashboard", "/admin/dashboard", "/api/dashboard", null, 1, true));
        MenuRecord.Create req = new MenuRecord.Create("Dashboard2", "/admin/dashboard", "/api/dashboard2", null, 2, true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> menuService.createMenu(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.MENU_ALREADY_EXISTS);
    }

    @Test
    void 메뉴를_트리_구조로_조회할_수_있다() {
        // given
        menuService.createMenu(new MenuRecord.Create("Dashboard", "/admin/dashboard", "/api/dashboard", null, 1, true));
        menuService.createMenu(new MenuRecord.Create("System", "/admin/system", "/api/system", null, 2, true));
        Long rootMenuId = findMenuByRoutePath("/admin/dashboard").id();
        menuService.createMenu(new MenuRecord.Create("DashboardUsers", "/admin/dashboard/users", "/api/dashboard/users", rootMenuId, 1, true));
        menuService.createMenu(new MenuRecord.Create("DashboardSettings", "/admin/dashboard/settings", "/api/dashboard/settings", rootMenuId, 2, true));

        // when
        List<MenuRecord.TreeResponse> tree = menuService.getMenuTree();

        // then
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).routePath()).isEqualTo("/admin/dashboard");
        assertThat(tree.get(0).children()).hasSize(2);
        assertThat(tree.get(0).children().get(0).routePath()).isEqualTo("/admin/dashboard/users");
        assertThat(tree.get(1).routePath()).isEqualTo("/admin/system");
    }

    @Test
    void 메뉴를_삭제하면_조회에_실패한다() {
        // given
        menuService.createMenu(new MenuRecord.Create("Users", "/admin/users", "/api/users", null, 1, true));
        Long menuId = findMenuByRoutePath("/admin/users").id();

        // when
        menuService.deleteMenu(menuId);
        CustomException exception = assertThrows(CustomException.class, () -> menuService.getMenu(menuId));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.MENU_NOT_FOUND);
    }

    private MenuRecord.Response findMenuByRoutePath(String routePath) {
        return menuService.getMenus()
                          .stream()
                          .filter(menu -> routePath.equals(menu.routePath()))
                          .findFirst()
                          .orElseThrow();
    }
}
