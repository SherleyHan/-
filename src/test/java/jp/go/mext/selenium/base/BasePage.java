package jp.go.mext.selenium.base;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;

/**
 * Page Object 基类：封装所有页面共享的等待、点击、输入、断言取值动作。
 *
 * <p>约定：
 * <ul>
 *   <li>所有元素定位统一使用 {@link By}，对外暴露为 public static final 常量</li>
 *   <li>所有交互都必须经过显式等待（{@link WebDriverWait}），禁止裸 findElement + 硬等待</li>
 *   <li>页面跳转由方法返回目标 Page Object，形成链式导航</li>
 * </ul>
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final String baseUrl;

    protected BasePage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    // ---------- 导航 ----------

    /** 打开相对路径页面（自动拼接 RANDOM_PORT 的服务地址） */
    protected BasePage openPath(String path) {
        driver.get(baseUrl + path);
        return this;
    }

    protected void waitForUrlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String pageTitle() {
        return driver.getTitle();
    }

    // ---------- 元素操作 ----------

    /** 等待元素出现在 DOM 中 */
    protected WebElement present(By by) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(by));
    }

    /** 等待元素可见 */
    protected WebElement visible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /** 等待元素可点击 */
    protected WebElement clickable(By by) {
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    protected void click(By by) {
        clickable(by).click();
    }

    /** 清空并输入文本（先 clear 再 sendKeys，避免残留旧值） */
    protected void type(By by, String text) {
        WebElement el = visible(by);
        el.clear();
        if (text != null && !text.isEmpty()) {
            el.sendKeys(text);
        }
    }

    protected String text(By by) {
        return visible(by).getText();
    }

    protected String attribute(By by, String name) {
        return present(by).getAttribute(name);
    }

    protected boolean exists(By by) {
        return !driver.findElements(by).isEmpty();
    }

    protected int count(By by) {
        return driver.findElements(by).size();
    }

    /** 上传本地文件（文件必须已存在于磁盘） */
    protected void attachFile(By fileInput, Path file) {
        visible(fileInput).sendKeys(file.toAbsolutePath().toString());
    }

    // ---------- 异步消息等待 ----------

    /**
     * 等待元素文本变为非空白后返回。
     * 用于页面上的 {@code #message} 之类先占位、后由 JS 填充的提示区域。
     */
    protected String waitForNonBlankText(By by) {
        return wait.until(d -> {
            WebElement el = d.findElement(by);
            String t = el.getText();
            return (t != null && !t.isBlank()) ? t : null;
        });
    }

    /** 等待元素文本包含指定片段 */
    protected void waitForTextContains(By by, String fragment) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(by, fragment));
    }

    /**
     * 等待元素文本包含指定片段并返回该文本。
     * 用于区分 AJAX 中间态（如「実行中...」）与最终结果。
     */
    protected String waitForTextContaining(By by, String fragment) {
        return wait.until(d -> {
            WebElement el = d.findElement(by);
            String t = el.getText();
            return (t != null && t.contains(fragment)) ? t : null;
        });
    }

    /** 等待元素 class 包含指定片段（用于区分 info / error 样式） */
    protected void waitForClassContains(By by, String fragment) {
        wait.until(d -> {
            String cls = d.findElement(by).getAttribute("class");
            return cls != null && cls.contains(fragment);
        });
    }

    protected void waitForElementCountAtLeast(By by, int expected) {
        wait.until(d -> d.findElements(by).size() >= expected);
    }

    // ---------- JS 辅助 ----------

    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    protected boolean isFormValid(String formId) {
        return Boolean.TRUE.equals(js("return document.getElementById(arguments[0]).checkValidity();", formId));
    }

    protected String validationMessageOf(String fieldId) {
        Object msg = js("return document.getElementById(arguments[0]).validationMessage;", fieldId);
        return msg == null ? "" : msg.toString();
    }
}
