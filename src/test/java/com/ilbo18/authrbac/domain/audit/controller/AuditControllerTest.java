package com.ilbo18.authrbac.domain.audit.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilbo18.authrbac.domain.audit.entity.Audit;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.repository.AuditRepository;
import com.ilbo18.authrbac.domain.auth.record.AuthRecord;
import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import com.ilbo18.authrbac.domain.menu.repository.MenuRepository;
import com.ilbo18.authrbac.domain.menu.service.MenuService;
import com.ilbo18.authrbac.domain.permission.entity.Permission;
import com.ilbo18.authrbac.domain.permission.record.PermissionRecord;
import com.ilbo18.authrbac.domain.permission.repository.PermissionRepository;
import com.ilbo18.authrbac.domain.permission.service.PermissionService;
import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.record.RoleRecord;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
import com.ilbo18.authrbac.domain.role.service.RoleService;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.record.UserRecord;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import com.ilbo18.authrbac.domain.user.service.UserService;
import com.ilbo18.authrbac.global.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 감사 로그 조회와 로그 적재 흐름을 검증한다.
 */
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 감사로그_목록과_단건을_조회할_수_있다() throws Exception {
        // given
        Role adminRole = createRole("AUDIT_ADMIN", "AuditAdmin");
        User adminUser = createUser("auditadmin", "Password1!", "Admin", adminRole.getId(), true);
        String accessToken = loginAndGetAccessToken("auditadmin", "Password1!");
        authenticateAs(adminUser);
        try {
            createAuditSourceData();
        } finally {
            SecurityContextHolder.clearContext();
        }
        List<Audit> audits = auditRepository.findAll();
        Audit permissionDeleteAudit = findAudit(AuditDomainType.PERMISSION, AuditActionType.DELETE);

        // then
        assertThat(audits).hasSize(12);
        assertThat(countByDomain(audits, AuditDomainType.ROLE)).isEqualTo(3L);
        assertThat(countByDomain(audits, AuditDomainType.MENU)).isEqualTo(3L);
        assertThat(countByDomain(audits, AuditDomainType.USER)).isEqualTo(3L);
        assertThat(countByDomain(audits, AuditDomainType.PERMISSION)).isEqualTo(3L);
        assertThat(countByAction(audits, AuditActionType.CREATE)).isEqualTo(4L);
        assertThat(countByAction(audits, AuditActionType.UPDATE)).isEqualTo(4L);
        assertThat(countByAction(audits, AuditActionType.DELETE)).isEqualTo(4L);
        assertThat(permissionDeleteAudit.getActorLoginId()).isEqualTo("auditadmin");
        assertThat(permissionDeleteAudit.getDescription()).isEqualTo("PERMISSION 삭제");

        // when
        ResultActions listResult = mockMvc.perform(get("/api/audits")
                                              .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        listResult.andExpect(status().isOk())
                  .andExpect(jsonPath("$.code").value(200))
                  .andExpect(jsonPath("$.data.length()").value(12));

        // when
        ResultActions detailResult = mockMvc.perform(get("/api/audits/{id}", permissionDeleteAudit.getId())
                                                .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        detailResult.andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(permissionDeleteAudit.getId()))
                    .andExpect(jsonPath("$.data.domainType").value("PERMISSION"))
                    .andExpect(jsonPath("$.data.actionType").value("DELETE"))
                    .andExpect(jsonPath("$.data.actorLoginId").value("auditadmin"));
    }

    @Test
    void 토큰이_없으면_감사로그_조회에_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/audits"));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3007"));
    }

    /** 각 도메인에서 create, update, delete 를 한 번씩 수행해 총 12건의 감사 로그를 만든다. */
    private void createAuditSourceData() {
        roleService.createRole(new RoleRecord.Create("AUDIT_ROLE", "AuditRole", "audit role", true));
        Role role = findRoleByCode("AUDIT_ROLE");
        roleService.updateRole(role.getId(), new RoleRecord.Update("AuditRole2", "audit role 2", true));

        menuService.createMenu(new MenuRecord.Create("Dashboard", "/dashboard", null, 1, true));
        Menu menu = findMenuByPath("/dashboard");
        menuService.updateMenu(menu.getId(), new MenuRecord.Update("Dashboard2", "/dashboard-main", null, 2, true));

        userService.createUser(new UserRecord.Create("audit01", "Password1!", "Auditor", role.getId(), true));
        User user = userRepository.findByLoginIdAndDeletedFalse("audit01");
        userService.updateUser(user.getId(), new UserRecord.Update("audit02", "Password2!", "Auditor2", role.getId(), true));

        permissionService.createPermission(new PermissionRecord.Create(role.getId(), menu.getId(), true, true, false, false, true));
        Permission permission = permissionRepository.findAllByDeletedFalse().get(0);
        permissionService.updatePermission(permission.getId(), new PermissionRecord.Update(role.getId(), menu.getId(), true, true, true, false, true));

        permissionService.deletePermission(permission.getId());
        userService.deleteUser(user.getId());
        menuService.deleteMenu(menu.getId());
        roleService.deleteRole(role.getId());
    }

    /** 관리자 로그인 fixture 는 감사 건수에 포함되면 안 되므로 repository.save 로 직접 저장한다. */
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

    /** 감사 로그 개수를 고정하기 위해 관리자 사용자도 repository.save 로 직접 저장한다. */
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

    /** 삭제 전 상태의 역할만 대상으로 찾아 테스트 대상을 명확히 식별한다. */
    private Role findRoleByCode(String code) {
        return roleRepository.findAllByDeletedFalse()
                             .stream()
                             .filter(role -> code.equals(role.getCode()))
                             .findFirst()
                             .orElseThrow();
    }

    /** 삭제 전 상태의 메뉴만 대상으로 찾아 테스트 대상을 명확히 식별한다. */
    private Menu findMenuByPath(String path) {
        return menuRepository.findAllByDeletedFalse()
                             .stream()
                             .filter(menu -> path.equals(menu.getPath()))
                             .findFirst()
                             .orElseThrow();
    }

    /** 도메인과 작업 유형으로 특정 감사 로그를 찾는다. */
    private Audit findAudit(AuditDomainType domainType, AuditActionType actionType) {
        return auditRepository.findAll()
                              .stream()
                              .filter(audit -> audit.getDomainType() == domainType)
                              .filter(audit -> audit.getActionType() == actionType)
                              .findFirst()
                              .orElseThrow();
    }

    /** 서비스 직접 호출에도 인증 사용자 문맥을 맞추기 위해 SecurityContext 를 수동으로 채운다. */
    private void authenticateAs(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getLoginId(), user.getRoleId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 로그인 후 access token 을 추출한다. */
    private String loginAndGetAccessToken(String loginId, String password) throws Exception {
        AuthRecord.Login req = new AuthRecord.Login(loginId, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content(objectMapper.writeValueAsString(req)))
                                  .andExpect(status().isOk())
                                  .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        return response.path("data").path("accessToken").asText();
    }

    /** 도메인 유형별 로그 수를 센다. */
    private long countByDomain(List<Audit> audits, AuditDomainType domainType) {
        return audits.stream()
                     .filter(audit -> audit.getDomainType() == domainType)
                     .count();
    }

    /** 작업 유형별 로그 수를 센다. */
    private long countByAction(List<Audit> audits, AuditActionType actionType) {
        return audits.stream()
                     .filter(audit -> audit.getActionType() == actionType)
                     .count();
    }

    /** Bearer 인증 헤더 값을 생성한다. */
    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
