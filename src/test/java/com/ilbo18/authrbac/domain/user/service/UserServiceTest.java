package com.ilbo18.authrbac.domain.user.service;

import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.record.UserRecord;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 사용자 서비스의 핵심 비즈니스 규칙을 검증한다.
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 사용자를_생성할_수_있다() {
        // given
        Role role = createRole("USER_CREATE", "UserCreate");
        UserRecord.Create req = new UserRecord.Create("tester1", "Password1!", "Tester", role.getId(), true);

        // when
        userService.createUser(req);

        // then
        User savedUser = userRepository.findByLoginIdAndDeletedFalse("tester1");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Tester");
        assertThat(savedUser.getRoleId()).isEqualTo(role.getId());
        assertThat(passwordEncoder.matches("Password1!", savedUser.getPassword())).isTrue();
    }

    @Test
    void 로그인아이디가_중복되면_사용자_생성에_실패한다() {
        // given
        Role role = createRole("USER_DUP", "UserDup");
        createUser("tester1", "Password1!", "Tester", role.getId(), true);
        UserRecord.Create req = new UserRecord.Create("TESTER1", "Password2!", "Tester2", role.getId(), true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> userService.createUser(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    void 존재하지_않는_역할이면_사용자_생성에_실패한다() {
        // given
        UserRecord.Create req = new UserRecord.Create("tester1", "Password1!", "Tester", 999L, true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> userService.createUser(req));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_ROLE);
    }

    @Test
    void 로그인아이디를_정규화해_저장한다() {
        // given
        Role role = createRole("USER_NORMALIZE", "UserNormalize");
        UserRecord.Create req = new UserRecord.Create("  TESTER1  ", "Password1!", "Tester", role.getId(), true);

        // when
        userService.createUser(req);

        // then
        User savedUser = userRepository.findByLoginIdAndDeletedFalse("tester1");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getLoginId()).isEqualTo("tester1");
    }

    @Test
    void 사용자를_수정할_수_있다() {
        // given
        Role sourceRole = createRole("USER_UPD_1", "UserUpd1");
        Role targetRole = createRole("USER_UPD_2", "UserUpd2");
        User user = createUser("tester1", "Password1!", "Tester", sourceRole.getId(), true);
        UserRecord.Update req = new UserRecord.Update("tester2", "Password2!", "Changed", targetRole.getId(), false);

        // when
        userService.updateUser(user.getId(), req);

        // then
        User updatedUser = userRepository.findByIdAndDeletedFalse(user.getId());
        assertThat(updatedUser.getLoginId()).isEqualTo("tester2");
        assertThat(updatedUser.getName()).isEqualTo("Changed");
        assertThat(updatedUser.getRoleId()).isEqualTo(targetRole.getId());
        assertThat(updatedUser.getEnabled()).isFalse();
        assertThat(passwordEncoder.matches("Password2!", updatedUser.getPassword())).isTrue();
    }

    @Test
    void 사용자를_삭제하면_조회에_실패한다() {
        // given
        Role role = createRole("USER_DEL", "UserDel");
        User user = createUser("tester1", "Password1!", "Tester", role.getId(), true);

        // when
        userService.deleteUser(user.getId());
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUser(user.getId()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        assertThat(userRepository.findByIdAndDeletedFalse(user.getId())).isNull();
    }

    /** 사용자 서비스가 아닌 선행 참조 데이터만 필요하므로 역할은 repository.save 로 직접 저장한다. */
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

    /** 중복/수정 시나리오의 기준 데이터를 만들기 위해 사용자는 repository.save 로 직접 저장한다. */
    private User createUser(String loginId, String rawPassword, String name, Long roleId, boolean enabled) {
        return userRepository.save(
            User.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .roleId(roleId)
                .enabled(enabled)
                .build()
        );
    }
}
