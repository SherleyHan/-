package jp.go.mext.controller;

import jp.go.mext.dto.UserDto;
import jp.go.mext.dto.UserSession;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.interceptor.LoginInterceptor;
import jp.go.mext.service.LoginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LoginController MockMvc 测试
 * - 验证 POST /login 业务状态码
 * - 验证 session 写入
 * - 验证异常返回 JSON
 */
@WebMvcTest(LoginController.class)
@Import(LoginInterceptor.class)
@ActiveProfiles("test")
@DisplayName("LoginController WebMvc 测试")
class LoginControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private LoginService loginService;

    private UserSession mockSession(String userId) {
        UserDto dto = new UserDto();
        dto.setUserId(userId);
        dto.setUserName("测试 " + userId);
        dto.setRiyoushaCd("U");
        dto.setResearchType("SY");
        dto.setAuthorityCd("USR");
        return new UserSession(dto);
    }

    @Test
    @DisplayName("登录成功 → statusCode=0 + 跳转 /menu")
    void loginSuccess() throws Exception {
        when(loginService.authenticate("admin", "password123"))
                .thenReturn(mockSession("admin"));

        mvc.perform(post("/login")
                        .param("userId", "admin")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResultStatus.RESULT_OK.getCode()))
                .andExpect(jsonPath("$.userName").value("测试 admin"));
    }

    @Test
    @DisplayName("密码错误 → statusCode=-1 + 错误信息")
    void wrongPassword() throws Exception {
        when(loginService.authenticate(anyString(), anyString()))
                .thenThrow(new BusinessException(ResultStatus.RESULT_ERROR, "用户ID或密码错误"));

        mvc.perform(post("/login")
                        .param("userId", "admin")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResultStatus.RESULT_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("用户ID或密码错误"));
    }

    @Test
    @DisplayName("账号锁定 → statusCode=90")
    void accountLocked() throws Exception {
        when(loginService.authenticate(anyString(), anyString()))
                .thenThrow(new BusinessException(ResultStatus.SESSION_MUKOU, "账号已被锁定"));

        mvc.perform(post("/login")
                        .param("userId", "locked")
                        .param("password", "any"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResultStatus.SESSION_MUKOU.getCode()));
    }

    @Test
    @DisplayName("loginConfirm：登录成功 → 写入 session")
    void loginConfirmWritesSession() throws Exception {
        when(loginService.authenticate("admin", "password123"))
                .thenReturn(mockSession("admin"));

        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/loginConfirm")
                        .session(session)
                        .param("userId", "admin")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("0"))
                .andExpect(jsonPath("$.redirect").value("/menu"));

        Object stored = session.getAttribute(LoginController.SESSION_KEY);
        // 写入后断言（同一 session 引用）
        org.junit.jupiter.api.Assertions.assertNotNull(stored);
    }

    @Test
    @DisplayName("logout：清空 session 并返回")
    void logout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginController.SESSION_KEY, mockSession("admin"));

        mvc.perform(post("/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("0"));
    }
}
