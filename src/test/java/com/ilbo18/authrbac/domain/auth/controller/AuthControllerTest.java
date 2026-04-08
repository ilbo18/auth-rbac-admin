package com.ilbo18.authrbac.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.repository.MenuRepository;
import com.ilbo18.authrbac.domain.permission.entity.Permission;
import com.ilbo18.authrbac.domain.permission.repository.PermissionRepository;
import com.ilbo18.authrbac.domain.role.entity.Role;
import com.ilbo18.authrbac.domain.role.repository.RoleRepository;
import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT 로그인과 보호 API 인가 흐름을 함께 검증한다.
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 로그인에_성공한다() throws Exception {
        // given
        Role role = createRole("AUTH_ADMIN", "AuthAdmin");
        createUser("authadmin", "Password1!", "Tester", role.getId(), true);

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of(
                                              "loginId", "authadmin",
                                              "password", "Password1!"
                                          ))));

        // then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data.accessToken").isString())
              .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
              .andExpect(jsonPath("$.data.loginId").value("authadmin"))
              .andExpect(jsonPath("$.data.roleId").value(role.getId()))
              .andExpect(jsonPath("$.data.name").value("Tester"));
    }

    @Test
    void 비밀번호가_일치하지_않으면_로그인에_실패한다() throws Exception {
        // given
        Role role = createRole("AUTH_USER", "AuthUser");
        createUser("authuser", "Password1!", "Tester", role.getId(), true);

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of(
                                              "loginId", "authuser",
                                              "password", "WrongPassword1!"
                                          ))));

        // then
        result.andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value("A3003"));
    }

    @Test
    void 존재하지_않는_로그인아이디면_로그인에_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of(
                                              "loginId", "missinguser",
                                              "password", "Password1!"
                                          ))));

        // then
        result.andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value("A3003"));
    }

    @Test
    void 공백과_대소문자를_정규화해_로그인한다() throws Exception {
        // given
        Role role = createRole("AUTH_NORMALIZE", "AuthNormalize");
        createUser("normalized", "Password1!", "Tester", role.getId(), true);

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of(
                                              "loginId", "  NORMALIZED  ",
                                              "password", "Password1!"
                                          ))));

        // then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.data.loginId").value("normalized"));
    }

    @Test
    void 유효한_토큰이_있으면_me_조회에_성공한다() throws Exception {
        // given
        Role role = createRole("AUTH_ME", "AuthMe");
        createUser("authme", "Password1!", "Tester", role.getId(), true);
        String accessToken = loginAndGetAccessToken("authme", "Password1!");

        // when
        ResultActions result = mockMvc.perform(get("/api/auth/me")
                                          .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data.loginId").value("authme"))
              .andExpect(jsonPath("$.data.roleId").value(role.getId()));
    }

    @Test
    void 토큰이_없으면_me_조회에_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/auth/me"));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3007"));
    }

    @Test
    void 잘못된_토큰이면_me_조회에_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/auth/me")
                                          .header(HttpHeaders.AUTHORIZATION, bearerToken("invalid-token")));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3004"));
    }

    @Test
    void 읽기_권한이_있으면_보호된_API_조회에_성공한다() throws Exception {
        // given
        Role role = createRole("AUTH_PERMISSION_ALLOW", "AuthPermissionAllow");
        createUser("permituser", "Password1!", "Tester", role.getId(), true);
        Menu userMenu = createMenu("Users", "/admin/users", "/api/users");
        createPermission(role.getId(), userMenu.getId(), true, false, false, false, true);
        String accessToken = loginAndGetAccessToken("permituser", "Password1!");

        // when
        ResultActions result = mockMvc.perform(get("/api/users")
                                          .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void 읽기_권한이_없으면_보호된_API_조회에_실패한다() throws Exception {
        // given
        Role role = createRole("AUTH_PERMISSION_DENY", "AuthPermissionDeny");
        createUser("denyuser", "Password1!", "Tester", role.getId(), true);
        createMenu("Users", "/admin/users", "/api/users");
        Menu roleMenu = createMenu("Roles", "/admin/roles", "/api/roles");
        createPermission(role.getId(), roleMenu.getId(), true, false, false, false, true);
        String accessToken = loginAndGetAccessToken("denyuser", "Password1!");

        // when
        ResultActions result = mockMvc.perform(get("/api/users")
                                          .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        result.andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("A1002"));
    }

    /** 인증 흐름만 검증하려고 선행 참조 데이터는 repository.save 로 직접 준비한다. */
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

    /** 로그인 대상만 빠르게 만들기 위해 사용자 fixture 도 repository.save 로 직접 준비한다. */
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

    /** 보호 API 인가만 검증하려고 메뉴는 routePath 와 apiPath 를 직접 저장한다. */
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

    /** CRUD 서비스까지 타면 audit 가 섞이므로 권한 fixture 는 repository.save 로 직접 준비한다. */
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

    private String loginAndGetAccessToken(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content(toJson(Map.of(
                                          "loginId", loginId,
                                          "password", password
                                      ))))
                                  .andExpect(status().isOk())
                                  .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        return response.path("data").path("accessToken").asText();
    }

    private String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
