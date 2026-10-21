package jp.go.mext.integration;

import jp.go.mext.dao.DirectionsExecutionDao;
import jp.go.mext.dao.NotificationMailDao;
import jp.go.mext.dto.DirectionsExecutionDto;
import jp.go.mext.enums.BatchStatus;
import jp.go.mext.enums.ResultStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.annotation.PostConstruct;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 通知邮件管理 → 数据库 → 批处理 全链路集成测试
 *
 * 流程：
 *   1. POST /loginConfirm（写入 session）
 *   2. POST /directions2030/detail/execute（写入 notification_mail + 创建 WAIT 指示）
 *   3. 直接 SQL / DAO 验证数据库状态
 *   4. 调用 BT_MA010101.executeOnce（批处理状态流转）
 *   5. 再次验证数据库状态 END
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("通知邮件管理 - 全链路集成测试")
class SurveyFlowIntegrationTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private NotificationMailDao mailDao;
    @Autowired private DirectionsExecutionDao executionDao;

    private MockMvc mvc;

    @PostConstruct
    void init() {
        this.mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private MockHttpSession loginAs(String userId) throws Exception {
        // data-test.sql 的密码哈希对应 password123
        MvcResult result = mvc.perform(post("/loginConfirm")
                        .param("userId", userId)
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("0"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        return session;
    }

    @Test
    @DisplayName("完整流程：登录 → 编辑 → 执行 → 数据库校验 → 批处理 → END")
    @Sql(scripts = "/db/reset-survey-test.sql",
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void fullFlow_success() throws Exception {
        // 1. 登录
        MockHttpSession session = loginAs("test_admin");
        assertNotNull(session.getAttribute("userSession"));

        // 2. 执行指示
        String body = "gamenSyubetu=SY&mailList=a%40example.com&mailList=b%40example.com";
        MvcResult execResult = mvc.perform(post("/directions2030/detail/execute")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResultStatus.RESULT_OK.getCode()))
                .andReturn();

        // 3. 验证数据库
        int mailCount = mailDao.countBySurveyType("SY");
        assertEquals(2, mailCount, "应该只有 2 条 SY 邮件记录");

        // 4. 找新创建的指示（用 mail_address 区分；data-test.sql 中 TEST_WAIT_001 是 a@example.com，
        //    新建的是 a@example.com:b@example.com）
        List<DirectionsExecutionDto> all = executionDao.findWaitList();
        DirectionsExecutionDto created = all.stream()
                .filter(d -> "SY".equals(d.getSurveyType()))
                .filter(d -> "a@example.com:b@example.com".equals(d.getMailAddress()))
                .filter(d -> BatchStatus.WAIT.getCode().equals(d.getStatus()))
                .findFirst()
                .orElse(null);
        assertNotNull(created, "应能找到新创建的 WAIT 指示");

        // 5. 直接验证 DAO 状态
        DirectionsExecutionDto dbExec = executionDao.findById(created.getExecutionId());
        assertNotNull(dbExec, "新创建的指示应能查到");
        assertEquals(BatchStatus.WAIT.getCode(), dbExec.getStatus());
        assertNotNull(dbExec.getFilePath(), "应生成文件路径");

        // 6. 验证生成的文件
        java.nio.file.Path filePath = java.nio.file.Paths.get(dbExec.getFilePath());
        assertTrue(java.nio.file.Files.exists(filePath), "邮件文件应已生成");
        String content = java.nio.file.Files.readString(filePath);
        assertTrue(content.contains("a@example.com"));
        assertTrue(content.contains("b@example.com"));
    }

    @Test
    @DisplayName("异常流程：未登录访问 execute → 跳转登录页")
    @Sql(scripts = "/db/reset-survey-test.sql",
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void executeWithoutLogin() throws Exception {
        mvc.perform(post("/directions2030/detail/execute")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("gamenSyubetu=SY&mailList=a%40example.com"))
                .andExpect(status().is3xxRedirection())
                // LoginInterceptor 未登录时重定向到 loginZ000（2026-09-28 起由 /login 调整而来）
                .andExpect(redirectedUrl("/loginZ000"));
    }

    @Test
    @DisplayName("异常流程：邮箱格式错误 → CHECK_ERROR + 数据库未变")
    @Sql(scripts = "/db/reset-survey-test.sql",
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void executeWithInvalidEmail() throws Exception {
        MockHttpSession session = loginAs("test_admin");

        int mailBefore = mailDao.countBySurveyType("SY");

        mvc.perform(post("/directions2030/detail/execute")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("gamenSyubetu=SY&mailList=not-an-email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResultStatus.CHECK_ERROR.getCode()));

        // 数据库不应该被改
        int mailAfter = mailDao.countBySurveyType("SY");
        assertEquals(mailBefore, mailAfter, "校验失败时不应写入数据库");
    }
}
