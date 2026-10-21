package jp.go.mext.batch;

import jp.go.mext.dao.DirectionsExecutionDao;
import jp.go.mext.dto.DirectionsExecutionDto;
import jp.go.mext.enums.BatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BT_MA010101 批处理单元测试")
class BT_MA010101Test {

    @Mock private DirectionsExecutionDao executionDao;
    private BT_MA010101 batch;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        batch = new BT_MA010101(executionDao);
        tempDir = Files.createTempDirectory("mext-batch-test");
    }

    private DirectionsExecutionDto newExec(String id, String surveyType) {
        DirectionsExecutionDto d = new DirectionsExecutionDto();
        d.setExecutionId(id);
        d.setSurveyType(surveyType);
        d.setBusinessType("NOTIFY_MAIL");
        d.setStatus(BatchStatus.WAIT.getCode());
        return d;
    }

    @Test
    @DisplayName("正常执行：mail_to_new.txt 替换为 mail_to.txt，状态 WAIT→RUN→END")
    void executeOnceNormal() throws IOException {
        // 准备文件
        Path dir = Files.createDirectories(tempDir.resolve("SY"));
        Files.writeString(dir.resolve("mail_to_new.txt"), "new@example.com");

        DirectionsExecutionDto exec = newExec("EXE_001", "SY");
        when(executionDao.findWaitList()).thenReturn(List.of(exec));
        when(executionDao.countByStatus(BatchStatus.RUN.getCode())).thenReturn(0);

        int n = batch.executeOnce(tempDir.toString());

        assertEquals(1, n);
        // 文件操作验证
        assertFalse(Files.exists(dir.resolve("mail_to_new.txt")), "原 new 文件应被移走");
        assertTrue(Files.exists(dir.resolve("mail_to.txt")), "cur 文件应已生成");
        assertEquals("new@example.com", Files.readString(dir.resolve("mail_to.txt")));

        // 状态机验证
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(executionDao, times(2)).updateStatus(eq("EXE_001"), statusCaptor.capture(),
                any(), any());
        assertEquals(BatchStatus.RUN.getCode(), statusCaptor.getAllValues().get(0));
        assertEquals(BatchStatus.END.getCode(), statusCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("已有 RUN 作业时跳过处理 → 状态保持 WAIT")
    void skipWhenAnotherRunning() {
        DirectionsExecutionDto exec = newExec("EXE_002", "SY");
        when(executionDao.findWaitList()).thenReturn(List.of(exec));
        when(executionDao.countByStatus(BatchStatus.RUN.getCode())).thenReturn(1); // 有作业在跑

        int n = batch.executeOnce(tempDir.toString());
        assertEquals(0, n);
        verify(executionDao, never()).updateStatus(eq("EXE_002"), anyString(), any(), any());
    }

    @Test
    @DisplayName("邮件文件缺失 → 异常并 ABEND")
    void fileMissingAbend() {
        DirectionsExecutionDto exec = newExec("EXE_003", "SY");
        when(executionDao.findWaitList()).thenReturn(List.of(exec));
        when(executionDao.countByStatus(BatchStatus.RUN.getCode())).thenReturn(0);

        // 不创建 mail_to_new.txt
        int n = batch.executeOnce(tempDir.toString());
        assertEquals(0, n, "失败应不计为成功");

        // 应尝试过 RUN，然后 ABEND
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(executionDao, times(2)).updateStatus(eq("EXE_003"), statusCaptor.capture(),
                any(), any());
        assertEquals(BatchStatus.RUN.getCode(), statusCaptor.getAllValues().get(0));
        assertEquals(BatchStatus.ABEND.getCode(), statusCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("无 WAIT 指示 → 直接返回 0")
    void noWaitExecutions() {
        when(executionDao.findWaitList()).thenReturn(List.of());

        int n = batch.executeOnce(tempDir.toString());
        assertEquals(0, n);
        verify(executionDao, never()).updateStatus(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("多次执行：每条 WAIT 独立处理")
    void multipleExecutions() throws IOException {
        Path dirA = Files.createDirectories(tempDir.resolve("HI"));
        Path dirB = Files.createDirectories(tempDir.resolve("HL"));
        Files.writeString(dirA.resolve("mail_to_new.txt"), "hi@example.com");
        Files.writeString(dirB.resolve("mail_to_new.txt"), "hl@example.com");

        when(executionDao.findWaitList()).thenReturn(List.of(
                newExec("EXE_A", "HI"),
                newExec("EXE_B", "HL")
        ));
        when(executionDao.countByStatus(BatchStatus.RUN.getCode())).thenReturn(0);

        int n = batch.executeOnce(tempDir.toString());
        assertEquals(2, n);
        assertTrue(Files.exists(dirA.resolve("mail_to.txt")));
        assertTrue(Files.exists(dirB.resolve("mail_to.txt")));
    }
}
