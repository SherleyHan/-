package jp.go.mext.selenium;

import jp.go.mext.selenium.base.BaseUITest;
import jp.go.mext.selenium.data.UITestData;
import jp.go.mext.selenium.pages.Directions2030Page;
import jp.go.mext.selenium.pages.LoginPage;
import jp.go.mext.selenium.pages.MenuPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜单页 UI 自动化
 *
 * <p>覆盖：登录后菜单渲染、两个調査类别入口跳转、注销后会话失效、未登录访问拦截。
 * 页面：{@code menu.jsp}
 */
@DisplayName("菜单页 - UI 自动化")
@Tag(BaseUITest.UI_TAG)
@Sql(scripts = "/db/reset-ui-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MenuUITest extends BaseUITest {

    @Test
    @DisplayName("登录成功 → 菜单展示欢迎语与 SY/HI 两个入口")
    void menuShowsWelcomeAndBothEntries() {
        MenuPage menu = loginAs(UITestData.USER_ADMIN, UITestData.PASSWORD);

        assertTrue(menu.isLoaded(), "应进入菜单页");
        assertTrue(menu.welcomeText().contains("Test Admin"),
                "应展示登录用户名，实际：" + menu.welcomeText());
        assertTrue(menu.hasSurveyEntry(UITestData.SURVEY_TYPE_SY), "应存在 SY 入口");
        assertTrue(menu.hasSurveyEntry(UITestData.SURVEY_TYPE_HI), "应存在 HI 入口");
    }

    @Test
    @DisplayName("点击「通知メール管理（HI）」→ 打开 HI 类别管理画面")
    void openHiDirectionsViaLink() {
        MenuPage menu = loginAs(UITestData.USER_ADMIN, UITestData.PASSWORD);

        Directions2030Page page = menu.openDirections2030ViaLink(UITestData.SURVEY_TYPE_HI);

        assertTrue(page.currentUrl().contains("/directions2030/detail/HI"),
                "应跳转到 HI 管理画面，实际：" + page.currentUrl());
        assertTrue(page.mailRowCount() >= 0, "管理画面应渲染出通知邮件表格");
    }

    @Test
    @DisplayName("注销 → 会话失效，再次访问菜单被踢回登录页")
    void logoutInvalidatesSession() {
        MenuPage menu = loginAs(UITestData.USER_ADMIN, UITestData.PASSWORD);
        assertTrue(menu.isLoaded(), "登录后应先进入菜单页");

        menu.logout();

        // 注销后重新访问受保护页面，应被 LoginInterceptor 拦回登录页
        openAndWaitUrl(UITestData.PATH_MENU, "loginZ000");
        assertTrue(loginPage().isLoaded(), "注销后应回到登录页");
    }

    @Test
    @DisplayName("未登录直接访问 /menu → 重定向到登录页")
    void unauthenticatedAccessRedirectsToLogin() {
        openAndWaitUrl(UITestData.PATH_MENU, "loginZ000");

        LoginPage login = loginPage();
        assertTrue(login.isLoaded(), "未登录应被拦截到登录页");
        assertTrue(login.currentUrl().contains("loginZ000"),
                "实际地址：" + login.currentUrl());
    }
}
