package com.ilbo18.authrbac.domain.permission.service;

import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.repository.MenuRepository;
import com.ilbo18.authrbac.domain.permission.entity.Permission;
import com.ilbo18.authrbac.domain.permission.record.PermissionRecord;
import com.ilbo18.authrbac.domain.permission.repository.PermissionRepository;
import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
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
 * 권한 서비스의 핵심 비즈니스 규칙을 검증한다.
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
class PermissionServiceTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Test
    void 권한을_생성할_수_있다() {
        // given
        Role role = createRole("PERM_CREATE", "PermCreate");
        Menu menu = createMenu("Dashboard", "/admin/dashboard", "/api/dashboard");
        PermissionRecord.Create req = new PermissionRecord.Create(role.getId(), menu.getId(), true, false, false, false, true);

        // when
        permissionService.createPermission(req);

        // then
        List<Permission> permissions = permissionRepository.findAllByDeletedFalse();
        assertThat(permissions).hasSize(1);
        assertThat(permissions.get(0).getRoleId()).isEqualTo(role.getId());
        assertThat(permissions.get(0).getMenuId()).isEqualTo(menu.getId());
        assertThat(permissions.get(0).getCanRead()).isTrue();
    }

    @Test
    void 역할과_메뉴_조합이_중복되면_권한_생성에_실패한다() {
        // given
        Role role = createRole("PERM_DUP", "PermDup");
        Menu menu = createMenu("Users", "/admin/users", "/api/users");
        PermissionRecord.Create req = new PermissionRecord.Create(role.getId(), menu.getId(), true, false, false, false, true);
        permissionService.createPermission(req);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> permissionService.createPermission(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PERMISSION_ALREADY_EXISTS);
    }

    @Test
    void 존재하지_않는_역할이면_권한_생성에_실패한다() {
        // given
        Menu menu = createMenu("Users", "/admin/users", "/api/users");
        PermissionRecord.Create req = new PermissionRecord.Create(999L, menu.getId(), true, false, false, false, true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> permissionService.createPermission(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_ROLE);
    }

    @Test
    void 존재하지_않는_메뉴면_권한_생성에_실패한다() {
        // given
        Role role = createRole("PERM_MENU", "PermMenu");
        PermissionRecord.Create req = new PermissionRecord.Create(role.getId(), 999L, true, false, false, false, true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> permissionService.createPermission(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_MENU);
    }

    @Test
    void 모든_권한이_false면_권한_생성에_실패한다() {
        // given
        Role role = createRole("PERM_ACT", "PermAct");
        Menu menu = createMenu("Users", "/admin/users", "/api/users");
        PermissionRecord.Create req = new PermissionRecord.Create(role.getId(), menu.getId(), false, false, false, false, true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> permissionService.createPermission(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_PERMISSION_ACTION);
    }

    @Test
    void 권한을_수정할_수_있다() {
        // given
        Role role = createRole("PERM_UPDATE", "PermUpdate");
        Menu sourceMenu = createMenu("Users", "/admin/users", "/api/users");
        Menu targetMenu = createMenu("Reports", "/admin/reports", "/api/reports");
        PermissionRecord.Create createReq = new PermissionRecord.Create(role.getId(), sourceMenu.getId(), true, false, false, false, true);
        permissionService.createPermission(createReq);
        Long permissionId = permissionRepository.findAllByDeletedFalse().get(0).getId();
        PermissionRecord.Update updateReq = new PermissionRecord.Update(role.getId(), targetMenu.getId(), true, true, true, false, false);

        // when
        permissionService.updatePermission(permissionId, updateReq);

        // then
        Permission updatedPermission = permissionRepository.findByIdAndDeletedFalse(permissionId);
        assertThat(updatedPermission.getMenuId()).isEqualTo(targetMenu.getId());
        assertThat(updatedPermission.getCanCreate()).isTrue();
        assertThat(updatedPermission.getCanUpdate()).isTrue();
        assertThat(updatedPermission.getEnabled()).isFalse();
    }

    @Test
    void 삭제한_권한은_같은_조합으로_다시_생성할_수_있다() {
        // given
        Role role = createRole("PERM_RECREATE", "PermRecreate");
        Menu menu = createMenu("Reports", "/admin/reports", "/api/reports");
        PermissionRecord.Create req = new PermissionRecord.Create(role.getId(), menu.getId(), true, false, false, false, true);
        permissionService.createPermission(req);
        Long permissionId = permissionRepository.findAllByDeletedFalse().get(0).getId();

        // when
        permissionService.deletePermission(permissionId);
        permissionService.createPermission(req);

        // then
        assertThat(permissionRepository.findAll()).hasSize(2);
        assertThat(permissionRepository.findAllByDeletedFalse()).hasSize(1);
    }

    /** 권한 검증에 필요한 참조 데이터만 준비하려고 역할은 직접 저장한다. */
    private Role createRole(String code, String name) {
        return roleRepository.save(
            Role.builder()
                .code(code)
                .name(name)
                .description("test role")
                .enabled(true)
                .build()
        );
    }

    /** 인가 경로 기준 검증이 목적이 아니므로 메뉴도 직접 저장한다. */
    private Menu createMenu(String name, String routePath, String apiPath) {
        return menuRepository.save(
            Menu.builder()
                .name(name)
                .routePath(routePath)
                .apiPath(apiPath)
                .parentId(null)
                .sortOrder(1)
                .enabled(true)
                .build()
        );
    }
}
