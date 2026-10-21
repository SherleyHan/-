package jp.go.mext.selenium.pages;

import jp.go.mext.selenium.base.BasePage;
import jp.go.mext.selenium.data.UITestData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;

/**
 * 通知邮件管理画面
 * {@code src/main/webapp/WEB-INF/jsp/directions2030/directions2030.jsp}
 *
 * <p>三个动作按钮的差异（业务上很关键，写用例时容易踩坑）：
 * <ul>
 *   <li>{@code #btnReflect} 反映：AJAX 上传文件，成功后用 {@code document.write} 重绘整页</li>
 *   <li>{@code #btnModify} 变更：原生表单提交 → 跳转确认页</li>
 *   <li>{@code #btnExecute} 执行：AJAX 返回 JSON，成功后 3 秒跳回菜单</li>
 * </ul>
 */
public class Directions2030Page extends BasePage {

    public static final By FILE_INPUT = By.id("uploadFile");
    public static final By BTN_REFLECT = By.id("btnReflect");
    public static final By BTN_DOWNLOAD = By.id("btnDownload");
    public static final By MAIL_TEXTAREA = By.id("mailAddressEdit");
    public static final By BTN_MODIFY = By.id("btnModify");
    public static final By BTN_EXECUTE = By.id("btnExecute");
    public static final By MESSAGE = By.id("message");
    /** 反映成功横幅；用精确 class 匹配，避开 #message 的 "message info" 样式 */
    public static final By INFO_BANNER = By.xpath("//div[@class='info']");
    public static final By MAIL_ROWS = By.cssSelector("table tbody tr");
    public static final By MAIL_CELLS = By.cssSelector("table tbody tr td:nth-child(3)");

    public Directions2030Page(WebDriver driver, WebDriverWait wait, String baseUrl) {
        super(driver, wait, baseUrl);
    }

    /** 打开指定調査类别的管理画面 */
    public Directions2030Page open(String surveyType) {
        openPath(UITestData.PATH_DIRECTIONS_PREFIX + surveyType);
        visible(MAIL_TEXTAREA);
        return this;
    }

    public Directions2030Page setMailAddress(String text) {
        type(MAIL_TEXTAREA, text);
        return this;
    }

    public String mailAddressValue() {
        return attribute(MAIL_TEXTAREA, "value");
    }

    // ---------- 反映（文件上传） ----------

    public Directions2030Page attachFile(Path file) {
        attachFile(FILE_INPUT, file);
        return this;
    }

    public Directions2030Page clickReflect() {
        click(BTN_REFLECT);
        return this;
    }

    /** 点击反映并等待前端拦截提示（未选文件 / 文件名非法 / 扩展名不对） */
    public String clickReflectAndExpectMessage(String expectedFragment) {
        click(BTN_REFLECT);
        return waitForTextContaining(MESSAGE, expectedFragment);
    }

    /** 反映成功：页面被 document.write 重绘，出现 reflected 横幅 */
    public Directions2030Page waitForReflectedBanner() {
        wait.ignoring(org.openqa.selenium.StaleElementReferenceException.class,
                        org.openqa.selenium.NoSuchElementException.class)
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(INFO_BANNER));
        return this;
    }

    // ---------- 变更 → 确认页 ----------
    /** 点击变更并期望跳转确认页 */
    public ConfirmPage clickModifyExpectingNavigation() {
        click(BTN_MODIFY);
        waitForUrlContains(UITestData.PATH_CONFIRM);
        return new ConfirmPage(driver, wait, baseUrl);
    }

    /** 点击变更并期望被前端校验拦住（停留在本页） */
    public Directions2030Page clickModifyExpectingValidationError() {
        click(BTN_MODIFY);
        return this;
    }

    /** 点击变更并等待指定的前端校验提示 */
    public String clickModifyAndExpectMessage(String expectedFragment) {
        click(BTN_MODIFY);
        return waitForTextContaining(MESSAGE, expectedFragment);
    }

    // ---------- 执行（AJAX） ----------

    public Directions2030Page clickExecute() {
        click(BTN_EXECUTE);
        return this;
    }

    /** 页内直接执行并等待成功提示（会先出现「実行中...」中间态，必须等到最终成功文案） */
    public String executeAndWaitSuccess() {
        click(BTN_EXECUTE);
        return waitForTextContaining(MESSAGE, UITestData.MSG_EXECUTE_SUCCESS);
    }

    // ---------- 压缩下载 ----------

    /** 触发压缩下载（JS 动态构造 form 并 submit） */
    public Directions2030Page clickDownload() {
        click(BTN_DOWNLOAD);
        return this;
    }

    // ---------- 断言辅助 ----------

    public String message() {
        return waitForNonBlankText(MESSAGE);
    }

    public String currentMessage() {
        return text(MESSAGE);
    }

    public boolean isMessageError() {
        String cls = attribute(MESSAGE, "class");
        return cls != null && cls.contains(UITestData.CSS_ERROR);
    }

    public boolean isReflectedBannerShown() {
        return exists(INFO_BANNER);
    }

    public String reflectedBannerText() {
        return text(INFO_BANNER);
    }

    public int mailRowCount() {
        return count(MAIL_ROWS);
    }

    public java.util.List<String> mailColumnValues() {
        return driver.findElements(MAIL_CELLS).stream()
                .map(org.openqa.selenium.WebElement::getText)
                .toList();
    }
}
