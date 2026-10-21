package jp.go.mext.selenium.pages;

import jp.go.mext.selenium.base.BasePage;
import jp.go.mext.selenium.data.UITestData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * 指示内容确认页
 * {@code src/main/webapp/WEB-INF/jsp/common/commonConfirm.jsp}
 *
 * <p>由通知邮件管理画面的「変更」按钮进入，展示变更后的邮箱列表。
 * 「実行」按钮走 AJAX 调 {@code /directions2030/detail/execute}。
 */
public class ConfirmPage extends BasePage {

    public static final By HEADING = By.xpath("//h1[contains(text(),'指示内容確認')]");
    public static final By BTN_EXECUTE = By.id("btnConfirmExecute");
    public static final By BTN_BACK = By.xpath("//button[normalize-space()='戻る']");
    public static final By MESSAGE = By.id("message");
    public static final By MAIL_ROWS = By.cssSelector("table tbody tr");
    public static final By MAIL_CELLS = By.cssSelector("table tbody tr td:nth-child(2)");
    public static final By HIDDEN_MAIL_INPUTS = By.cssSelector("input[type='hidden'][name='mailList']");

    public ConfirmPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        super(driver, wait, baseUrl);
    }

    public boolean isLoaded() {
        return exists(HEADING);
    }

    public int mailRowCount() {
        return count(MAIL_ROWS);
    }

    public List<String> mailAddresses() {
        return driver.findElements(MAIL_CELLS).stream()
                .map(WebElement::getText)
                .toList();
    }

    /** 表单实际提交时携带的邮箱（hidden input） */
    public List<String> submittedMailAddresses() {
        return driver.findElements(HIDDEN_MAIL_INPUTS).stream()
                .map(el -> el.getAttribute("value"))
                .toList();
    }

    /**
     * 点击実行并接受原生确认框（「本当に実行しますか？」）。
     * Selenium 4 不会自动处理原生对话框，必须显式 switchTo().alert().accept()。
     */
    public ConfirmPage clickExecuteAndAcceptConfirm() {
        click(BTN_EXECUTE);
        acceptConfirmDialog();
        return this;
    }

    /** 点击実行并在确认框选择「取消」 */
    public ConfirmPage clickExecuteAndDismissConfirm() {
        click(BTN_EXECUTE);
        dismissConfirmDialog();
        return this;
    }

    /** 点击実行 → 接受确认框 → 等待执行成功提示（过滤「実行中...」中间态） */
    public String executeAndWaitSuccess() {
        clickExecuteAndAcceptConfirm();
        return waitForTextContaining(MESSAGE, UITestData.MSG_EXECUTE_SUCCESS);
    }

    private void acceptConfirmDialog() {
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    private void dismissConfirmDialog() {
        wait.until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    public String currentMessage() {
        return text(MESSAGE);
    }

    public boolean isMessageError() {
        String cls = attribute(MESSAGE, "class");
        return cls != null && cls.contains(UITestData.CSS_ERROR);
    }

    /** 失败分支下按钮不应触发跳转 */
    public void clickBack() {
        click(BTN_BACK);
    }
}
