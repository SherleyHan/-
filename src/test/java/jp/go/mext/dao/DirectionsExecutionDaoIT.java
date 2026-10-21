package jp.go.mext.dao;

import jp.go.mext.dto.DirectionsExecutionDto;
import jp.go.mext.enums.BatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DirectionsExecutionDao 集成测试
 * 验证状态机 SQL 行为：insert → updateStatus (WAIT→RUN→END)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DirectionsExecutionDao 集成测试")
class DirectionsExecutionDaoIT {

    @Autowired
    private DirectionsExecutionDao dao;

    @Test
    @DisplayName("插入 + 查询 byId")
    void insertAndFindById() {
        DirectionsExecutionDto dto = new DirectionsExecutionDto();
        dto.setExecutionId("DAO_TEST_001");
        dto.setSurveyType("SY");
        dto.setBusinessType("NOTIFY_MAIL");
        dto.setStatus(BatchStatus.WAIT.getCode());
        dto.setMailAddress("test@x.com");
        dto.setCreateUser("tester");

        int n = dao.insert(dto);
        assertEquals(1, n);

        DirectionsExecutionDto found = dao.findById("DAO_TEST_001");
        assertNotNull(found);
        assertEquals(BatchStatus.WAIT.getCode(), found.getStatus());
        assertEquals("NOTIFY_MAIL", found.getBusinessType());
    }

    @Test
    @DisplayName("状态流转：WAIT → RUN → END")
    void statusTransition() {
        DirectionsExecutionDto dto = new DirectionsExecutionDto();
        dto.setExecutionId("DAO_TEST_TRANS");
        dto.setSurveyType("SY");
        dto.setBusinessType("NOTIFY_MAIL");
        dto.setStatus(BatchStatus.WAIT.getCode());
        dto.setCreateUser("tester");
        dao.insert(dto);

        // WAIT → RUN
        dao.updateStatus("DAO_TEST_TRANS", BatchStatus.RUN.getCode(), null, "BATCH");
        DirectionsExecutionDto cur = dao.findById("DAO_TEST_TRANS");
        assertEquals(BatchStatus.RUN.getCode(), cur.getStatus());
        assertNotNull(cur.getStartTime(), "RUN 状态应写入 start_time");

        // RUN → END
        dao.updateStatus("DAO_TEST_TRANS", BatchStatus.END.getCode(), null, "BATCH");
        DirectionsExecutionDto end = dao.findById("DAO_TEST_TRANS");
        assertEquals(BatchStatus.END.getCode(), end.getStatus());
        assertNotNull(end.getEndTime(), "END 状态应写入 end_time");
    }

    @Test
    @DisplayName("状态流转：RUN → ABEND，errorMessage 应保留")
    void abendKeepsErrorMessage() {
        DirectionsExecutionDto dto = new DirectionsExecutionDto();
        dto.setExecutionId("DAO_TEST_ABEND");
        dto.setSurveyType("HI");
        dto.setBusinessType("NOTIFY_MAIL");
        dto.setStatus(BatchStatus.RUN.getCode());
        dto.setCreateUser("tester");
        dao.insert(dto);

        dao.updateStatus("DAO_TEST_ABEND", BatchStatus.ABEND.getCode(),
                "邮件文件不存在", "BATCH");

        DirectionsExecutionDto cur = dao.findById("DAO_TEST_ABEND");
        assertEquals(BatchStatus.ABEND.getCode(), cur.getStatus());
        assertEquals("邮件文件不存在", cur.getErrorMessage());
    }

    @Test
    @DisplayName("findWaitList：data-test.sql 预置一条 WAIT 记录")
    void findWaitList() {
        List<DirectionsExecutionDto> list = dao.findWaitList();
        assertTrue(list.stream().anyMatch(d -> "TEST_WAIT_001".equals(d.getExecutionId())));
    }

    @Test
    @DisplayName("countByStatus")
    void countByStatus() {
        int waitCount = dao.countByStatus("WAIT");
        assertTrue(waitCount >= 1, "data-test.sql 至少有一条 WAIT");
    }

    @Test
    @DisplayName("updateFilePath：写入后 findById 能读到")
    void updateFilePath() {
        dao.updateFilePath("TEST_WAIT_001", "/tmp/mail/SY/mail_to_new.txt");
        DirectionsExecutionDto d = dao.findById("TEST_WAIT_001");
        assertEquals("/tmp/mail/SY/mail_to_new.txt", d.getFilePath());
    }
}
