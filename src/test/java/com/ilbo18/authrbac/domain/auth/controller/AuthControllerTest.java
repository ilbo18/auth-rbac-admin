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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT 로그인과 Redis refresh token 흐름, 보호 API 인가를 함께 검증한다.
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

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;
    private Map<String, String> redisValues;

    @BeforeEach
    void setUpRedisMock() {
        redisValues = new ConcurrentHashMap<>();
        valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisValues.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(stringRedisTemplate.delete(anyString())).thenAnswer(invocation -> redisValues.remove(invocation.getArgument(0)) != null);
    }

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
              .andExpect(jsonPath("$.data.refreshToken").isString())
              .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
              .andExpect(jsonPath("$.data.accessTokenExpiresIn").isNumber())
              .andExpect(jsonPath("$.data.refreshTokenExpiresIn").isNumber())
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
    void refreshToken으로_accessToken을_재발급한다() throws Exception {
        // given
        Role role = createRole("AUTH_REISSUE", "AuthReissue");
        createUser("reissueuser", "Password1!", "Tester", role.getId(), true);
        AuthTokens tokens = loginAndGetTokens("reissueuser", "Password1!");

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of("refreshToken", tokens.refreshToken()))));

        // then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.data.accessToken").isString())
              .andExpect(jsonPath("$.data.refreshToken").isString())
              .andExpect(jsonPath("$.data.refreshToken").value(org.hamcrest.Matchers.not(tokens.refreshToken())));
    }

    @Test
    void 재발급에_사용한_refreshToken은_다시_쓸_수_없다() throws Exception {
        // given
        Role role = createRole("AUTH_ROTATE", "AuthRotate");
        createUser("rotateuser", "Password1!", "Tester", role.getId(), true);
        AuthTokens tokens = loginAndGetTokens("rotateuser", "Password1!");
        AuthTokens reissuedTokens = reissueAndGetTokens(tokens.refreshToken());

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of("refreshToken", tokens.refreshToken()))));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3008"));
        org.assertj.core.api.Assertions.assertThat(reissuedTokens.refreshToken()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void 다시_로그인하면_이전_refreshToken은_무효화된다() throws Exception {
        // given
        Role role = createRole("AUTH_RELOGIN", "AuthRelogin");
        createUser("reloginuser", "Password1!", "Tester", role.getId(), true);
        AuthTokens firstTokens = loginAndGetTokens("reloginuser", "Password1!");
        AuthTokens secondTokens = loginAndGetTokens("reloginuser", "Password1!");

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of("refreshToken", firstTokens.refreshToken()))));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3008"));
        org.assertj.core.api.Assertions.assertThat(secondTokens.refreshToken()).isNotEqualTo(firstTokens.refreshToken());
    }

    @Test
    void 잘못된_refreshToken이면_재발급에_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(toJson(Map.of("refreshToken", "invalid-refresh-token"))));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3008"));
    }

    @Test
    void 유효한_토큰이_있으면_me_조회에_성공한다() throws Exception {
        // given
        Role role = createRole("AUTH_ME", "AuthMe");
        createUser("authme", "Password1!", "Tester", role.getId(), true);
        String accessToken = loginAndGetTokens("authme", "Password1!").accessToken();

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
    void 인증된_사용자는_로그아웃할_수_있다() throws Exception {
        // given
        Role role = createRole("AUTH_LOGOUT", "AuthLogout");
        createUser("logoutuser", "Password1!", "Tester", role.getId(), true);
        AuthTokens tokens = loginAndGetTokens("logoutuser", "Password1!");

        // when
        ResultActions logoutResult = mockMvc.perform(post("/api/auth/logout")
                                                .header(HttpHeaders.AUTHORIZATION, bearerToken(tokens.accessToken())));
        ResultActions reissueResult = mockMvc.perform(post("/api/auth/reissue")
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content(toJson(Map.of("refreshToken", tokens.refreshToken()))));

        // then
        logoutResult.andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        reissueResult.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("A3008"));
    }

    @Test
    void 토큰_없이_로그아웃하면_실패한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/logout"));

        // then
        result.andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value("A3007"));
    }

    @Test
    void 읽기_권한이_있으면_보호된_API_조회에_성공한다() throws Exception {
        // given
        Role role = createRole("AUTH_PERMISSION_ALLOW", "AuthPermissionAllow");
        createUser("permituser", "Password1!", "Tester", role.getId(), true);
        Menu userMenu = createMenu("Users", "/admin/users", "/api/users");
        createPermission(role.getId(), userMenu.getId(), true, false, false, false, true);
        String accessToken = loginAndGetTokens("permituser", "Password1!").accessToken();

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
        String accessToken = loginAndGetTokens("denyuser", "Password1!").accessToken();

        // when
        ResultActions result = mockMvc.perform(get("/api/users")
                                          .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)));

        // then
        result.andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("A1002"));
    }

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

    private AuthTokens loginAndGetTokens(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content(toJson(Map.of(
                                          "loginId", loginId,
                                          "password", password
                                      ))))
                                  .andExpect(status().isOk())
                                  .andReturn();

        return extractTokens(result);
    }

    private AuthTokens reissueAndGetTokens(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/reissue")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content(toJson(Map.of("refreshToken", refreshToken))))
                                  .andExpect(status().isOk())
                                  .andReturn();

        return extractTokens(result);
    }

    private AuthTokens extractTokens(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        return new AuthTokens(
            response.path("data").path("accessToken").asText(),
            response.path("data").path("refreshToken").asText()
        );
    }

    private String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record AuthTokens(String accessToken, String refreshToken) {
    }
}
