package com.ilbo18.authrbac.domain.role.service;

import com.ilbo18.authrbac.domain.role.record.RoleRecord;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 역할 서비스의 기본 CRUD 흐름을 검증한다.
 */
@SpringBootTest
@Transactional
class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Test
    void 역할을_생성하고_조회할_수_있다() {
        // given
        RoleRecord.Create req = new RoleRecord.Create("ROLE_TEST", "RoleTest", "role description", true);

        // when
        roleService.createRole(req);
        RoleRecord.Response savedRole = findRoleByCode("ROLE_TEST");
        RoleRecord.Response foundRole = roleService.getRole(savedRole.id());

        // then
        assertThat(savedRole.code()).isEqualTo("ROLE_TEST");
        assertThat(foundRole.name()).isEqualTo("RoleTest");
        assertThat(foundRole.enabled()).isTrue();
    }

    @Test
    void 역할을_수정할_수_있다() {
        // given
        roleService.createRole(new RoleRecord.Create("ROLE_UPD", "RoleUpd", "before update", true));
        Long roleId = findRoleByCode("ROLE_UPD").id();
        RoleRecord.Update req = new RoleRecord.Update("RoleUpdChanged", "after update", false);

        // when
        roleService.updateRole(roleId, req);
        RoleRecord.Response updatedRole = roleService.getRole(roleId);

        // then
        assertThat(updatedRole.name()).isEqualTo("RoleUpdChanged");
        assertThat(updatedRole.description()).isEqualTo("after update");
        assertThat(updatedRole.enabled()).isFalse();
    }

    @Test
    void 역할을_삭제하면_조회에_실패한다() {
        // given
        roleService.createRole(new RoleRecord.Create("ROLE_DEL", "RoleDel", "delete role", true));
        Long roleId = findRoleByCode("ROLE_DEL").id();

        // when
        roleService.deleteRole(roleId);
        CustomException exception = assertThrows(CustomException.class, () -> roleService.getRole(roleId));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_NOT_FOUND);
    }

    /** 코드로 역할을 찾아 테스트 대상을 명확히 식별한다. */
    private RoleRecord.Response findRoleByCode(String code) {
        return roleService.getRoles()
                          .stream()
                          .filter(role -> code.equals(role.code()))
                          .findFirst()
                          .orElseThrow();
    }
}
