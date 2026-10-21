package jp.go.mext.selenium;

import jp.go.mext.selenium.base.BaseUITest;
import jp.go.mext.selenium.data.UITestData;
import jp.go.mext.selenium.pages.ConfirmPage;
import jp.go.mext.selenium.pages.Directions2030Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通知邮件管理画面 UI 自动化
 *
 * <p>覆盖：初始邮件列表渲染、文件反映、邮箱必填/格式校验、文件必填/文件名/扩展名校验、
 * 变更跳转确认页、页内直接执行、压缩下载。
 * 页面：{@code directions2030/directions2030.jsp}
 */
@DisplayName("通知邮件管理 - UI 自动化")
@Tag(BaseUITest.UI_TAG)
@Sql(scripts = "/db/reset-ui-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class Directions2030UITest extends BaseUITest {

    private static final String EXPECTED_MAIL_FILE = "mail_to_new.txt";
    private static final String EXPORT_FILE_NAME = "mail_to_new.txt";

    @TempDir
    Path uploadDir;

    // ---------- 画面渲染 ----------

    @Test
    @DisplayName("画面渲染：初始邮箱列表来自数据库（1 条）")
    void rendersInitialMailList() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);

        assertEquals(1, page.mailRowCount(), "data-test.sql 中 SY 仅配置 1 条邮件记录");
        assertTrue(page.currentUrl().contains("/directions2030/detail/SY"),
                "实际地址：" + page.currentUrl());
    }

    // ---------- 反映（文件上传） ----------

    @Test
    @DisplayName("反映：上传合法 txt → 重绘页面并回填 2 条邮箱")
    void reflectValidFileUpdatesMailTable() throws IOException {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);
        Path file = createFile(EXPECTED_MAIL_FILE,
                UITestData.MAIL_A + "\n" + UITestData.MAIL_B + "\n");

        page.attachFile(file).clickReflect().waitForReflectedBanner();

        assertTrue(page.reflectedBannerText().contains(UITestData.MSG_REFLECTED),
                "应提示内容已反映，实际：" + page.reflectedBannerText());
        assertEquals(2, page.mailRowCount(), "文件内 2 个邮箱应回填为 2 行");
    }

    // ---------- 邮箱校验 ----------

    @Test
    @DisplayName("邮箱必填：清空后点击变更 → 前端拦截")
    void emptyMailAddressBlocked() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);

        String message = page.setMailAddress("")
                .clickModifyAndExpectMessage(UITestData.MSG_MAIL_EMPTY);

        assertTrue(message.contains(UITestData.MSG_MAIL_EMPTY), "实际提示：" + message);
        assertTrue(page.isMessageError(), "提示应带 error 样式");
    }

    @Test
    @DisplayName("邮箱格式校验：非法地址被前端拦截")
    void invalidMailFormatBlocked() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);

        String message = page.setMailAddress(UITestData.MAIL_INVALID)
                .clickModifyAndExpectMessage(UITestData.MSG_MAIL_INVALID);

        assertTrue(message.contains(UITestData.MAIL_INVALID),
                "提示应带上非法地址，实际：" + message);
    }

    @Test
    @DisplayName("邮箱多值：冒号分隔的多个合法地址 → 跳转确认页并逐个回显")
    void multipleMailAddressesNavigateToConfirmPage() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);

        String mails = UITestData.MAIL_A + UITestData.MAIL_SEPARATOR + UITestData.MAIL_B;
        ConfirmPage confirm = page.setMailAddress(mails).clickModifyExpectingNavigation();

        assertTrue(confirm.isLoaded(), "应进入指示内容确认页");
        assertEquals(2, confirm.mailRowCount(), "确认页应回显 2 条邮箱");
        assertTrue(confirm.mailAddresses().contains(UITestData.MAIL_A), "应包含第 1 个邮箱");
        assertTrue(confirm.mailAddresses().contains(UITestData.MAIL_B), "应包含第 2 个邮箱");
    }

    // ---------- 文件校验 ----------

    @Test
    @DisplayName("文件必填：未选择文件点击反映 → 前端拦截")
    void noFileSelectedBlocked() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);

        String message = page.clickReflectAndExpectMessage(UITestData.MSG_FILE_REQUIRED);

        assertTrue(message.contains(UITestData.MSG_FILE_REQUIRED), "实际提示：" + message);
    }

    @Test
    @DisplayName("文件名校验：含空格与特殊字符的文件名被拦截")
    void illegalFileNameBlocked() throws IOException {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);
        Path file = createFile("bad name!.txt", UITestData.MAIL_A);

        String message = page.attachFile(file)
                .clickReflectAndExpectMessage(UITestData.MSG_FILE_NAME_INVALID);

        assertTrue(message.contains(UITestData.MSG_FILE_NAME_INVALID), "实际提示：" + message);
    }

    @Test
    @DisplayName("扩展名校验：非 .txt 文件被拦截")
    void wrongFileExtensionBlocked() throws IOException {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);
        Path file = createFile("data.log", UITestData.MAIL_A);

        String message = page.attachFile(file)
                .clickReflectAndExpectMessage(UITestData.MSG_FILE_EXT_INVALID);

        assertTrue(message.contains(UITestData.MSG_FILE_EXT_INVALID), "实际提示：" + message);
    }

    // ---------- 执行 & 下载 ----------

    @Test
    @DisplayName("页内执行：点击执行 → AJAX 返回成功提示与指示ID")
    void executeInlineShowsSuccess() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);

        String message = page.setMailAddress(UITestData.MAIL_A).executeAndWaitSuccess();

        assertTrue(message.contains(UITestData.MSG_EXECUTE_SUCCESS), "实际提示：" + message);
        assertTrue(message.contains(UITestData.MSG_EXECUTE_ORDER_ID), "应返回指示ID，实际：" + message);
    }

    @Test
    @DisplayName("压缩下载：点击下载 → 浏览器落盘待发送文件")
    void compressionDownloadProducesFile() throws IOException {
        // 1) 先执行一次，确保服务端已生成待下载文件
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);
        page.setMailAddress(UITestData.MAIL_A).executeAndWaitSuccess();

        // 2) 重新打开画面（避开执行成功后 3 秒自动跳菜单），再触发下载
        page = page.open(UITestData.SURVEY_TYPE_SY);
        clearDownloadDir();
        page.clickDownload();

        Path downloaded = waitForDownload(EXPORT_FILE_NAME);
        assertTrue(Files.isRegularFile(downloaded) && Files.size(downloaded) > 0,
                "应下载到非空文件：" + downloaded);
    }

    // ---------- 辅助 ----------

    private Path createFile(String fileName, String content) throws IOException {
        Path file = uploadDir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
