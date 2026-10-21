package jp.go.mext.dao;

import jp.go.mext.dto.NotificationMailDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationMailDao 集成测试
 * - @MybatisTest：自动配置内存数据库 + 加载 schema.sql
 * - 验证 SQL 真的能跑通 + 数据一致性
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationMailDao 集成测试（真实 SQL 执行）")
class NotificationMailDaoIT {

    @Autowired
    private NotificationMailDao dao;

    @Test
    @DisplayName("按 surveyType 查询 → data-test.sql 已插入 SY 数据")
    void findBySurveyType() {
        List<NotificationMailDto> list = dao.findBySurveyType("SY");
        assertNotNull(list);
        assertFalse(list.isEmpty(), "测试数据应有 SY 记录");
        assertEquals("SY", list.get(0).getSurveyType());
    }

    @Test
    @DisplayName("按 surveyType 查询 → 不存在时返回空列表")
    void findBySurveyTypeEmpty() {
        List<NotificationMailDto> list = dao.findBySurveyType("ZZ");
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("按 surveyType 删除 → 后续查询为空")
    void deleteBySurveyType() {
        dao.deleteBySurveyType("SY");
        List<NotificationMailDto> list = dao.findBySurveyType("SY");
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("插入 + 查询一条记录")
    void insertAndQuery() {
        NotificationMailDto dto = new NotificationMailDto();
        dto.setSurveyType("ZZ");
        dto.setMailAddress("zz@example.com");
        dto.setFileName("mail_to_new.txt");
        dto.setFileExt("txt");
        dto.setUpdateUser("test_user");

        int n = dao.insert(dto);
        assertEquals(1, n);
        assertNotNull(dto.getId(), "insert 后应回填主键");

        List<NotificationMailDto> list = dao.findBySurveyType("ZZ");
        assertEquals(1, list.size());
        assertEquals("zz@example.com", list.get(0).getMailAddress());
    }

    @Test
    @DisplayName("批量插入 → 多条记录一次写入")
    void batchInsert() {
        dao.deleteBySurveyType("SY");

        NotificationMailDto a = new NotificationMailDto();
        a.setSurveyType("SY"); a.setMailAddress("a@x.com");
        a.setFileName("mail_to_new.txt"); a.setFileExt("txt"); a.setUpdateUser("u");

        NotificationMailDto b = new NotificationMailDto();
        b.setSurveyType("SY"); b.setMailAddress("b@x.com");
        b.setFileName("mail_to_new.txt"); b.setFileExt("txt"); b.setUpdateUser("u");

        int n = dao.batchInsert(List.of(a, b));
        assertEquals(2, n);

        assertEquals(2, dao.countBySurveyType("SY"));
    }

    @Test
    @DisplayName("countBySurveyType：测试数据已预置 SY 记录")
    void count() {
        int c = dao.countBySurveyType("SY");
        assertTrue(c >= 1, "data-test.sql 应至少插入一条 SY 记录");
    }
}
