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
 * 감사 로그 적재와 검색 API를 함께 검증한다.
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
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
        Menu auditMenu = createMenu("Audits", "/admin/audits", "/api/audits");
        createPermission(adminRole.getId(), auditMenu.getId(), true, false, false, false, true);
        String accessToken = loginAndGetAccessToken("auditadmin", "Password1!");

        authenticateAs(adminUser);
        try {
            createAuditSourceData();
        } finally {
            SecurityContextHolder.clearContext();
        }

        List<Audit> audits = auditRepository.findAll();
        Audit permissionDeleteAudit = findAudit(AuditDomainType.PERMISSION, AuditActionType.DELETE, "PERMISSION 삭제");

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

        // when
        ResultActions listResult = mockMvc.perform(get("/api/audits")
                                              .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        listResult.andExpect(status().isOk())
                  .andExpect(jsonPath("$.code").value(200))
                  .andExpect(jsonPath("$.data.content.length()").value(12))
                  .andExpect(jsonPath("$.data.totalElements").value(12))
                  .andExpect(jsonPath("$.data.first").value(true));

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
    void 감사로그를_조건검색하고_페이징할_수_있다() throws Exception {
        // given
        Role adminRole = createRole("AUDIT_ADMIN_SEARCH", "AuditAdminSearch");
        User adminUser = createUser("auditadmin", "Password1!", "Admin", adminRole.getId(), true);
        Menu auditMenu = createMenu("Audits", "/admin/audits", "/api/audits");
        createPermission(adminRole.getId(), auditMenu.getId(), true, false, false, false, true);
        String accessToken = loginAndGetAccessToken("auditadmin", "Password1!");

        authenticateAs(adminUser);
        try {
            createAuditSourceData();
        } finally {
            SecurityContextHolder.clearContext();
        }

        // when
        ResultActions result = mockMvc.perform(get("/api/audits")
                                          .queryParam("page", "0")
                                          .queryParam("size", "2")
                                          .queryParam("domainType", "ROLE")
                                          .queryParam("actorLoginId", "auditadmin")
                                          .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data.content.length()").value(2))
              .andExpect(jsonPath("$.data.totalElements").value(3))
              .andExpect(jsonPath("$.data.totalPages").value(2))
              .andExpect(jsonPath("$.data.content[0].domainType").value("ROLE"))
              .andExpect(jsonPath("$.data.content[0].actorLoginId").value("auditadmin"));
    }

    @Test
    void 토큰이_없으면_감사로그_조회에_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/audits"));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3007"));
    }

    /** 각 도메인에서 create, update, delete 를 한 번씩 수행해 12건의 감사 로그를 만든다. */
    private void createAuditSourceData() {
        roleService.createRole(new RoleRecord.Create("AUDIT_ROLE", "AuditRole", "audit role", true));
        Role role = findRoleByCode("AUDIT_ROLE");
        roleService.updateRole(role.getId(), new RoleRecord.Update("AuditRole2", "audit role 2", true));

        menuService.createMenu(new MenuRecord.Create("Dashboard", "/admin/dashboard", "/api/dashboard", null, 1, true));
        Menu menu = findMenuByRoutePath("/admin/dashboard");
        menuService.updateMenu(menu.getId(), new MenuRecord.Update("Dashboard2", "/admin/dashboard/main", "/api/dashboard/main", null, 2, true));

        userService.createUser(new UserRecord.Create("audit01", "Password1!", "Auditor", role.getId(), true));
        User user = userRepository.findByLoginIdAndDeletedFalse("audit01");
        userService.updateUser(user.getId(), new UserRecord.Update("audit02", "Password2!", "Auditor2", role.getId(), true));

        permissionService.createPermission(new PermissionRecord.Create(role.getId(), menu.getId(), true, true, false, false, true));
        Permission permission = permissionRepository.findAllByDeletedFalse()
                                                    .stream()
                                                    .filter(item -> role.getId().equals(item.getRoleId()))
                                                    .filter(item -> menu.getId().equals(item.getMenuId()))
                                                    .findFirst()
                                                    .orElseThrow();
        permissionService.updatePermission(permission.getId(), new PermissionRecord.Update(role.getId(), menu.getId(), true, true, true, false, true));

        permissionService.deletePermission(permission.getId());
        userService.deleteUser(user.getId());
        menuService.deleteMenu(menu.getId());
        roleService.deleteRole(role.getId());
    }

    /** 감사 건수를 고정하려고 관리자 역할은 repository.save 로 직접 준비한다. */
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

    /** 로그인용 관리자 사용자는 audit 개수에 섞이면 안 되므로 직접 저장한다. */
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

    /** 감사 조회 권한만 추가하려고 메뉴는 repository.save 로 직접 준비한다. */
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

    /** 권한 CRUD를 타면 감사 건수가 늘어나므로 audit 조회 권한은 직접 저장한다. */
    private Permission createPermission(Long roleId, Long menuId, boolean canRead, boolean canCreate, boolean canUpdate, boolean canDelete, boolean enabled) {
        return permissionRepository.save(
            Permission.builder()
                .roleId(roleId)
                .menuId(menuId)
                .canRead(canRead)
                .canCreate(canCreate)
                .canUpdate(canUpdate)
                .canDelete(canDelete)
                .enabled(enabled)
                .build()
        );
    }

    private Role findRoleByCode(String code) {
        return roleRepository.findAllByDeletedFalse()
                             .stream()
                             .filter(role -> code.equals(role.getCode()))
                             .findFirst()
                             .orElseThrow();
    }

    private Menu findMenuByRoutePath(String routePath) {
        return menuRepository.findAllByDeletedFalse()
                             .stream()
                             .filter(menu -> routePath.equals(menu.getRoutePath()))
                             .findFirst()
                             .orElseThrow();
    }

    private Audit findAudit(AuditDomainType domainType, AuditActionType actionType, String description) {
        return auditRepository.findAll()
                              .stream()
                              .filter(audit -> audit.getDomainType() == domainType)
                              .filter(audit -> audit.getActionType() == actionType)
                              .filter(audit -> description.equals(audit.getDescription()))
                              .findFirst()
                              .orElseThrow();
    }

    private void authenticateAs(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getLoginId(), user.getRoleId());
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

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

    private long countByDomain(List<Audit> audits, AuditDomainType domainType) {
        return audits.stream()
                     .filter(audit -> audit.getDomainType() == domainType)
                     .count();
    }

    private long countByAction(List<Audit> audits, AuditActionType actionType) {
        return audits.stream()
                     .filter(audit -> audit.getActionType() == actionType)
                     .count();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
