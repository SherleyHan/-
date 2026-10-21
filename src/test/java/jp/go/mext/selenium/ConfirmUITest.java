package jp.go.mext.selenium;

import jp.go.mext.selenium.base.BaseUITest;
import jp.go.mext.selenium.data.UITestData;
import jp.go.mext.selenium.pages.ConfirmPage;
import jp.go.mext.selenium.pages.Directions2030Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 指示内容确认页 UI 自动化
 *
 * <p>覆盖：邮箱列表回显、隐藏域提交内容、戻る返回、実行成功并自动跳回菜单。
 * 页面：{@code common/commonConfirm.jsp}
 */
@DisplayName("指示内容确认页 - UI 自动化")
@Tag(BaseUITest.UI_TAG)
@Sql(scripts = "/db/reset-ui-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ConfirmUITest extends BaseUITest {

    private static final String TWO_MAILS =
            UITestData.MAIL_A + UITestData.MAIL_SEPARATOR + UITestData.MAIL_B;

    /** 登录 → 打开管理画面 → 变更 → 进入确认页 */
    private ConfirmPage openConfirmPage() {
        Directions2030Page page = loginAndOpenDirections2030(UITestData.SURVEY_TYPE_SY);
        return page.setMailAddress(TWO_MAILS).clickModifyExpectingNavigation();
    }

    @Test
    @DisplayName("确认页回显：表格展示 2 条邮箱，隐藏域同步提交值")
    void confirmPageShowsMailList() {
        ConfirmPage confirm = openConfirmPage();

        assertTrue(confirm.isLoaded(), "应显示「指示内容確認」标题");
        assertEquals(2, confirm.mailRowCount(), "应回显 2 条邮箱");

        List<String> rows = confirm.mailAddresses();
        assertTrue(rows.contains(UITestData.MAIL_A) && rows.contains(UITestData.MAIL_B),
                "表格内容应包含两个邮箱，实际：" + rows);

        List<String> hidden = confirm.submittedMailAddresses();
        assertEquals(2, hidden.size(), "隐藏域应携带 2 个 mailList 值，实际：" + hidden);
        assertTrue(hidden.contains(UITestData.MAIL_A) && hidden.contains(UITestData.MAIL_B),
                "隐藏域内容应正确，实际：" + hidden);
    }

    @Test
    @DisplayName("返回：点击戻る 回到通知邮件管理画面")
    void backButtonReturnsToManagementPage() {
        ConfirmPage confirm = openConfirmPage();

        confirm.clickBack();
        waitForUrlContains(UITestData.PATH_DIRECTIONS_PREFIX + UITestData.SURVEY_TYPE_SY);

        assertTrue(driver.getCurrentUrl().contains(UITestData.PATH_DIRECTIONS_PREFIX),
                "应回到管理画面，实际：" + driver.getCurrentUrl());
    }

    @Test
    @DisplayName("执行：点击実行 → 提示执行成功并在 3 秒后跳回菜单")
    void executeSuccessThenRedirectsToMenu() {
        ConfirmPage confirm = openConfirmPage();

        String message = confirm.executeAndWaitSuccess();
        assertTrue(message.contains(UITestData.MSG_EXECUTE_SUCCESS), "实际提示：" + message);

        waitForUrlContains(UITestData.PATH_MENU);
        assertTrue(driver.getCurrentUrl().contains(UITestData.PATH_MENU),
                "执行成功后应跳回菜单，实际：" + driver.getCurrentUrl());
    }

    @Test
    @DisplayName("执行：确认框选择取消 → 不发起请求，仍停留在确认页")
    void executeCancelledDoesNotSubmit() {
        ConfirmPage confirm = openConfirmPage();

        confirm.clickExecuteAndDismissConfirm();

        assertEquals("", confirm.currentMessage(), "取消后不应出现任何提示");
        assertTrue(confirm.isLoaded(), "应仍停留在确认页");
        assertFalse(confirm.isMessageError(), "取消不应产生错误提示");
    }
}
