package jp.go.mext.selenium.pages;

import jp.go.mext.selenium.base.BasePage;
import jp.go.mext.selenium.data.UITestData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * 登录页 {@code src/main/webapp/WEB-INF/jsp/login/loginZ000.jsp}
 *
 * <p>页面行为：表单 submit 被 JS 接管，先做本地校验，再 AJAX 调 {@code /login}，
 * 成功后调 {@code /loginConfirm} 写 session 并跳转 {@code /menu}。
 */
public class LoginPage extends BasePage {

    public static final By USER_ID = By.id("userId");
    public static final By PASSWORD = By.id("password");
    public static final By LOGIN_BUTTON = By.id("btnLogin");
    public static final By MESSAGE = By.id("message");

    public LoginPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        super(driver, wait, baseUrl);
    }

    /** 打开登录页（经由 {@code /login} → forward → {@code /loginZ000}） */
    public LoginPage open() {
        openPath(UITestData.PATH_LOGIN_FORM);
        visible(USER_ID);
        return this;
    }

    public boolean isLoaded() {
        return exists(USER_ID);
    }

    public LoginPage inputUserId(String userId) {
        type(USER_ID, userId);
        return this;
    }

    public LoginPage inputPassword(String password) {
        type(PASSWORD, password);
        return this;
    }

    public LoginPage submit() {
        click(LOGIN_BUTTON);
        return this;
    }

    /** 登录成功：密码正确后应跳转到菜单页 */
    public MenuPage loginAs(String userId, String password) {
        inputUserId(userId).inputPassword(password).submit();
        waitForUrlContains(UITestData.PATH_MENU);
        return new MenuPage(driver, wait, baseUrl);
    }

    /** 提交并停留在当前页（用于断言前端同步校验提示） */
    public LoginPage submitExpectingFailure(String userId, String password) {
        inputUserId(userId).inputPassword(password).submit();
        return this;
    }

    /**
     * 提交并等待指定的服务端提示。
     * 登录走 AJAX，会先出现「ログイン中...」中间态，必须等到最终文案出现。
     */
    public String submitAndExpectMessage(String userId, String password, String expectedFragment) {
        inputUserId(userId).inputPassword(password).submit();
        return waitForTextContaining(MESSAGE, expectedFragment);
    }

    public String message() {
        return waitForNonBlankText(MESSAGE);
    }

    public String currentMessage() {
        return text(MESSAGE);
    }

    public boolean isMessageError() {
        return attribute(MESSAGE, "class").contains(UITestData.CSS_ERROR);
    }

    /** 空提交后浏览器原生校验是否拦住表单 */
    public boolean isFormValid() {
        return isFormValid(UITestData.ID_LOGIN_FORM);
    }

    /** 浏览器原生校验提示（对应 HTML5 required） */
    public String userIdValidationMessage() {
        return validationMessageOf(UITestData.ID_USER_ID);
    }

    public String passwordValidationMessage() {
        return validationMessageOf(UITestData.ID_PASSWORD);
    }
}
