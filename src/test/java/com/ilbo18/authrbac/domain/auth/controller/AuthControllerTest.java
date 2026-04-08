package com.ilbo18.authrbac.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 인증 API 동작을 검증한다.
 */
@SpringBootTest
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

    /** 인증 흐름만 검증하려고 선행 역할 데이터는 repository.save 로 직접 저장한다. */
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

    /** 로그인 대상 사용자만 준비하려고 사용자 fixture 도 repository.save 로 직접 저장한다. */
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

    /** 로그인 응답에서 access token 을 추출한다. */
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

    /** JSON 요청 본문을 생성한다. */
    private String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /** Bearer 인증 헤더 값을 생성한다. */
    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
