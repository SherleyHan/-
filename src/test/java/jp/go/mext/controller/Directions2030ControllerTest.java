package jp.go.mext.controller;

import jp.go.mext.dto.Directions2030Form;
import jp.go.mext.dto.Directions2030Result;
import jp.go.mext.dto.UserSession;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.service.Directions2030Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Directions2030Controller MockMvc 测试
 * - 验证各端点路由
 * - 验证未登录访问被拒绝
 * - 验证执行接口返回业务状态码
 */
@WebMvcTest(Directions2030Controller.class)
@ActiveProfiles("test")
@DisplayName("Directions2030Controller WebMvc 测试")
class Directions2030ControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private Directions2030Service service;

    private MockHttpSession loggedInSession;

    @BeforeEach
    void setUp() {
        loggedInSession = new MockHttpSession();
        UserSession us = new UserSession();
        us.setUserId("admin");
        us.setUserName("管理员");
        us.setResearchType("SY");
        us.setAuthorityCd("ADM");
        loggedInSession.setAttribute(LoginController.SESSION_KEY, us);
    }

    @Test
    @DisplayName("未登录访问 → SESSION_MUKOU")
    void noSession() throws Exception {
        when(service.init(anyString())).thenReturn(new Directions2030Form());

        mvc.perform(post("/directions2030/detail/SY"))
                .andExpect(status().is3xxRedirection())
                // LoginInterceptor 未登录时重定向到 loginZ000（2026-09-28 起由 /login 调整而来）
                .andExpect(redirectedUrl("/loginZ000"));
    }

    @Test
    @DisplayName("已登录访问 detail → 返回 directions2030 视图")
    void detailSuccess() throws Exception {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setGamenId("directions2030_SY");
        form.setGamenName("通知邮件管理-SY");
        when(service.init("SY")).thenReturn(form);

        mvc.perform(post("/directions2030/detail/SY").session(loggedInSession))
                .andExpect(status().isOk())
                .andExpect(view().name("directions2030/directions2030"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @DisplayName("execute：成功 → statusCode=0")
    void executeSuccess() throws Exception {
        when(service.execute(any(Directions2030Form.class), anyString()))
                .thenReturn(Directions2030Result.of(ResultStatus.RESULT_OK, "执行成功 EXE_001"));

        mvc.perform(post("/directions2030/detail/execute")
                        .session(loggedInSession)
                        .param("gamenSyubetu", "SY")
                        .param("mailList", "a@example.com")
                        .param("mailList", "b@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("0"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("EXE_001")));
    }

    @Test
    @DisplayName("execute：邮箱非法 → CHECK_ERROR")
    void executeCheckError() throws Exception {
        when(service.execute(any(Directions2030Form.class), anyString()))
                .thenThrow(new BusinessException(ResultStatus.CHECK_ERROR, "邮箱格式不正确"));

        mvc.perform(post("/directions2030/detail/execute")
                        .session(loggedInSession)
                        .param("gamenSyubetu", "SY")
                        .param("mailList", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("1"));
    }

    @Test
    @DisplayName("execute：用户无调查权限 → SESSION_MUKOU")
    void executeNoAuthority() throws Exception {
        when(service.execute(any(Directions2030Form.class), anyString()))
                .thenThrow(new BusinessException(ResultStatus.SESSION_MUKOU, "用户无调查权限"));

        mvc.perform(post("/directions2030/detail/execute")
                        .session(loggedInSession)
                        .param("gamenSyubetu", "SY")
                        .param("mailList", "a@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("90"));
    }
}
