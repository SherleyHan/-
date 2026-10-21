package jp.go.mext.selenium;

import jp.go.mext.selenium.base.BaseUITest;
import jp.go.mext.selenium.data.UITestData;
import jp.go.mext.selenium.pages.LoginPage;
import jp.go.mext.selenium.pages.MenuPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录页 UI 自动化
 *
 * <p>覆盖：必填校验、非法字符校验、密码错误、账号锁定、用户不存在、登录成功跳转。
 * 页面：{@code login/loginZ000.jsp}
 */
@DisplayName("登录页 - UI 自动化")
@Tag(BaseUITest.UI_TAG)
@Sql(scripts = "/db/reset-ui-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class LoginUITest extends BaseUITest {

    @Test
    @DisplayName("必填校验：用户ID为空 → 浏览器原生 required 拦截提交")
    void requiredUserIdBlocksSubmit() {
        LoginPage page = loginPage().open();
        page.inputPassword(UITestData.PASSWORD).submit();

        assertFalse(page.isFormValid(), "用户ID为空时表单应校验失败");
        assertFalse(page.userIdValidationMessage().isBlank(), "应给出用户ID必填提示");
        assertTrue(page.currentUrl().contains("loginZ000"), "被拦截时不应离开登录页");
    }

    @Test
    @DisplayName("必填校验：密码为空 → 浏览器原生 required 拦截提交")
    void requiredPasswordBlocksSubmit() {
        LoginPage page = loginPage().open();
        page.inputUserId(UITestData.USER_ADMIN).submit();

        assertFalse(page.isFormValid(), "密码为空时表单应校验失败");
        assertFalse(page.passwordValidationMessage().isBlank(), "应给出密码必填提示");
        assertTrue(page.currentUrl().contains("loginZ000"), "被拦截时不应离开登录页");
    }

    @Test
    @DisplayName("格式校验：用户ID含非法字符 → 前端提示并带 error 样式")
    void illegalUserIdCharactersShowsError() {
        LoginPage page = loginPage().open();
        page.submitExpectingFailure("abc$%", UITestData.PASSWORD);

        String message = page.message();
        assertTrue(message.contains(UITestData.MSG_LOGIN_ILLEGAL_USER_ID),
                "应提示用户ID含非法字符，实际：" + message);
        assertTrue(page.isMessageError(), "提示应带 error 样式");
    }

    @Test
    @DisplayName("密码错误 → 停留登录页并提示错误")
    void wrongPasswordShowsError() {
        LoginPage page = loginPage().open();
        String message = page.submitAndExpectMessage(
                UITestData.USER_ADMIN, "wrong-password", UITestData.MSG_LOGIN_FAILED);

        assertTrue(message.contains(UITestData.MSG_LOGIN_FAILED),
                "应提示用户ID或密码错误，实际：" + message);
        assertTrue(page.isMessageError(), "提示应带 error 样式");
        assertTrue(page.currentUrl().contains("loginZ000"), "失败时不应跳转");
    }

    @Test
    @DisplayName("账号锁定 → 提示账号已被锁定")
    void lockedAccountShowsError() {
        LoginPage page = loginPage().open();
        String message = page.submitAndExpectMessage(
                UITestData.USER_LOCKED, UITestData.PASSWORD, UITestData.MSG_ACCOUNT_LOCKED);

        assertTrue(message.contains(UITestData.MSG_ACCOUNT_LOCKED),
                "应提示账号已锁定，实际：" + message);
    }

    @Test
    @DisplayName("用户不存在 → 提示用户ID或密码错误")
    void unknownUserShowsError() {
        LoginPage page = loginPage().open();
        String message = page.submitAndExpectMessage(
                UITestData.USER_UNKNOWN, UITestData.PASSWORD, UITestData.MSG_LOGIN_FAILED);

        assertTrue(message.contains(UITestData.MSG_LOGIN_FAILED),
                "不存在用户不应泄露具体原因，实际：" + message);
    }

    @Test
    @DisplayName("密码正确 → 跳转菜单页并展示用户名")
    void successRedirectsToMenu() {
        MenuPage menu = loginPage().open().loginAs(UITestData.USER_ADMIN, UITestData.PASSWORD);

        assertTrue(menu.isLoaded(), "应进入菜单页");
        assertTrue(menu.welcomeText().contains("Test Admin"),
                "菜单页应展示登录用户名，实际：" + menu.welcomeText());
    }
}
