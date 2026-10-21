package jp.go.mext.selenium.pages;

import jp.go.mext.selenium.base.BasePage;
import jp.go.mext.selenium.data.UITestData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * 主菜单页 {@code src/main/webapp/WEB-INF/jsp/menu.jsp}
 */
public class MenuPage extends BasePage {

    public static final By WELCOME_TEXT = By.xpath("//p[contains(text(),'ようこそ')]");
    public static final By LINK_DIRECTIONS_SY = By.linkText("通知メール管理（SY）");
    public static final By LINK_DIRECTIONS_HI = By.linkText("通知メール管理（HI）");
    public static final By BUTTON_DIRECTIONS = By.xpath("//button[normalize-space()='通知邮件管理页']");
    public static final By LOGOUT_BUTTON = By.xpath("//button[normalize-space()='ログアウト']");

    public MenuPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        super(driver, wait, baseUrl);
    }

    /** 未登录直接访问 /menu 会被拦截并跳回登录页，因此这里不主动断言，交由调用方判断 */
    public MenuPage open() {
        openPath(UITestData.PATH_MENU);
        return this;
    }

    public boolean isLoaded() {
        return exists(WELCOME_TEXT);
    }

    /** 菜单中是否存在指定調査类别的通知邮件管理入口 */
    public boolean hasSurveyEntry(String surveyType) {
        return exists(UITestData.SURVEY_TYPE_HI.equals(surveyType) ? LINK_DIRECTIONS_HI : LINK_DIRECTIONS_SY);
    }

    public String welcomeText() {
        return text(WELCOME_TEXT);
    }

    /** 从菜单进入通知邮件管理画面 */
    public Directions2030Page openDirections2030ViaLink(String surveyType) {
        By link = UITestData.SURVEY_TYPE_HI.equals(surveyType) ? LINK_DIRECTIONS_HI : LINK_DIRECTIONS_SY;
        click(link);
        waitForUrlContains(UITestData.PATH_DIRECTIONS_PREFIX);
        return new Directions2030Page(driver, wait, baseUrl);
    }

    /**
     * 注销。
     * 注意：{@code /logout} 是 {@code @RestController}，页面用的是原生表单提交，
     * 因此浏览器会停留在 JSON 响应页，这里只等待 URL 变化。
     */
    public void logout() {
        click(LOGOUT_BUTTON);
        waitForUrlContains("/logout");
    }
}
