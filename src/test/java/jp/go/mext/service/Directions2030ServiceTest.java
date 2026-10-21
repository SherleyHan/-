package jp.go.mext.service;

import jp.go.mext.dao.DirectionsExecutionDao;
import jp.go.mext.dao.NotificationMailDao;
import jp.go.mext.dto.Directions2030Form;
import jp.go.mext.dto.Directions2030Result;
import jp.go.mext.dto.DirectionsExecutionDto;
import jp.go.mext.enums.BatchStatus;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Directions2030Service 单元测试（Mockito 隔离 DAO）")
class Directions2030ServiceTest {

    @Mock private NotificationMailDao mailDao;
    @Mock private DirectionsExecutionDao executionDao;
    @Mock private LoginService loginService;

    private Directions2030Service service;

    @BeforeEach
    void setUp() {
        service = new Directions2030Service(mailDao, executionDao, loginService);
        ReflectionTestUtils.setField(service, "tempDir", System.getProperty("java.io.tmpdir"));
        ReflectionTestUtils.setField(service, "mailFolder", System.getProperty("java.io.tmpdir") + "/mext-mail");
    }

    // ---------- init ----------
    @Test
    @DisplayName("init：调查类别非法 → CHECK_ERROR")
    void initWithInvalidType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.init("INVALID"));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
    }

    @Test
    @DisplayName("init：调查类别 SY 正常返回 Form")
    void initSuccess() {
        Directions2030Form form = service.init("SY");
        assertEquals("SY", form.getGamenSyubetu());
        assertEquals("directions2030_SY", form.getGamenId());
        assertNotNull(form.getGamenName());
        verify(mailDao, times(1)).findBySurveyType("SY");
    }

    // ---------- reflect ----------
    @Test
    @DisplayName("reflect：空文件 → CHECK_ERROR")
    void reflectEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "uploadFile", "mail_to_new.txt", "text/plain", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reflect("SY", empty));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
    }

    @Test
    @DisplayName("reflect：文件名包含非法字符 → CHECK_ERROR")
    void reflectInvalidFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "uploadFile", "bad name.txt", "text/plain", "user@example.com".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reflect("SY", file));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
    }

    @Test
    @DisplayName("reflect：合法文件 → 解析邮件并返回 Form")
    void reflectSuccess() throws Exception {
        // 上传文件内容：每行一个邮箱
        String content = "a@example.com\nb@example.com\ninvalid-line\nc@example.com";
        MockMultipartFile file = new MockMultipartFile(
                "uploadFile", "mail_to_new.txt", "text/plain", content.getBytes());

        // 让 findFileConfig 返回符合预期的文件名校验
        jp.go.mext.dto.NotificationMailDto config = new jp.go.mext.dto.NotificationMailDto();
        config.setFileName("mail_to_new.txt");
        config.setFileExt("txt");
        when(mailDao.findFileConfig("SY")).thenReturn(config);

        Directions2030Form form = service.reflect("SY", file);

        assertEquals(3, form.getMailList().size(), "应过滤掉非法行");
        assertEquals("a@example.com", form.getMailList().get(0));
    }

    // ---------- modify ----------
    @Test
    @DisplayName("modify：邮件为空 → CHECK_ERROR")
    void modifyEmpty() {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setMailAddressEdit("");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.modify(form));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
    }

    @Test
    @DisplayName("modify：邮箱格式错误 → CHECK_ERROR")
    void modifyInvalidEmail() {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setMailAddressEdit("valid@example.com:not-an-email");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.modify(form));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
    }

    @Test
    @DisplayName("modify：合法邮箱列表 → 解析为 List")
    void modifySuccess() {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setMailAddressEdit("a@example.com : b@example.com");

        Directions2030Form result = service.modify(form);
        assertEquals(2, result.getMailList().size());
        assertEquals("a@example.com", result.getMailList().get(0));
        assertEquals("b@example.com", result.getMailList().get(1));
    }

    // ---------- execute ----------
    @Test
    @DisplayName("execute：写入 notification_mail + 创建 batch WAIT")
    void executeSuccess() {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setMailList(List.of("a@example.com", "b@example.com"));

        jp.go.mext.dto.NotificationMailDto config = new jp.go.mext.dto.NotificationMailDto();
        config.setFileName("mail_to_new.txt");
        config.setFileExt("txt");
        when(mailDao.findFileConfig("SY")).thenReturn(config);
        when(loginService.generateExecutionId()).thenReturn("TEST_EXEC_001");

        Directions2030Result result = service.execute(form, "test_user");

        assertTrue(result.isOk());
        // 验证：先 delete 再 batchInsert
        verify(mailDao, times(1)).deleteBySurveyType("SY");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<jp.go.mext.dto.NotificationMailDto>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(mailDao, times(1)).batchInsert(captor.capture());
        assertEquals(2, captor.getValue().size());

        // 验证：execution insert + updateFilePath
        ArgumentCaptor<DirectionsExecutionDto> execCaptor =
                ArgumentCaptor.forClass(DirectionsExecutionDto.class);
        verify(executionDao, times(1)).insert(execCaptor.capture());
        assertEquals("TEST_EXEC_001", execCaptor.getValue().getExecutionId());
        assertEquals(BatchStatus.WAIT.getCode(), execCaptor.getValue().getStatus());
        verify(executionDao, times(1)).updateFilePath(eq("TEST_EXEC_001"), anyString());
    }

    @Test
    @DisplayName("execute：邮件列表为空 → CHECK_ERROR")
    void executeEmpty() {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setMailList(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.execute(form, "test_user"));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
        verify(executionDao, never()).insert(any());
    }

    @Test
    @DisplayName("execute：邮箱格式错误 → CHECK_ERROR，且 DB 未写入")
    void executeInvalidEmail() {
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu("SY");
        form.setMailList(List.of("a@example.com", "bad-email"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.execute(form, "test_user"));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
        verify(mailDao, never()).batchInsert(anyList());
        verify(executionDao, never()).insert(any());
    }
}
