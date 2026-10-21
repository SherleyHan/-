package jp.go.mext.selenium.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import jp.go.mext.selenium.data.UITestData;
import jp.go.mext.selenium.pages.Directions2030Page;
import jp.go.mext.selenium.pages.LoginPage;
import jp.go.mext.selenium.pages.MenuPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * UI 自动化测试基类。
 *
 * <p>职责：
 * <ul>
 *   <li>以 {@code RANDOM_PORT} 启动真实内嵌 Tomcat（含 Jasper），保证 JSP 被真实渲染</li>
 *   <li>用 WebDriverManager 自动匹配 ChromeDriver，无需手工安装驱动</li>
 *   <li>统一 WebDriver 生命周期：每个测试方法一个干净的浏览器实例</li>
 *   <li>失败自动截图到 {@code target/ui-screenshots/}，便于排查</li>
 * </ul>
 *
 * <p>运行方式（默认不参与 {@code mvn test}，见 pom.xml 的 surefire exclude）：
 * <pre>
 *   mvn test -Pselenium                        # 无头执行全部 UI 用例
 *   mvn test -Pselenium -Dselenium.headless=false   # 打开浏览器观察
 *   mvn test -Pselenium -Dtest=LoginUITest          # 只跑某个类
 * </pre>
 *
 * <p>测试账号来自 {@code src/main/resources/db/data-test.sql}，见 {@link UITestData}。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // UI 用例使用独立的 H2 内存库：
                // 其它 @SpringBootTest 上下文共用 jdbc:h2:mem:mexttest，
                // 若两者同库，spring.sql.init 会把 data-test.sql 重复执行导致主键冲突。
                "spring.datasource.url=jdbc:h2:mem:mextuitest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        }
)
@ActiveProfiles("test")
public abstract class BaseUITest {

    /** 显式等待超时时间；CI 上网络慢时可调大 */
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    /** UI 自动化测试的 JUnit 5 标签；与 pom.xml 的 ui.tests.tag 保持一致 */
    public static final String UI_TAG = "ui";

    private static final String SCREENSHOT_DIR = "target/ui-screenshots";

    /** 浏览器统一下载目录，便于断言压缩下载等下载类用例 */
    protected static final Path DOWNLOAD_DIR = Paths.get("target", "ui-downloads");

    @LocalServerPort
    protected int port;

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeAll
    static void prepareDriverBinary() {
        // 只下载/校验一次 ChromeDriver，缓存在 ~/.cache/selenium
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void startBrowser() {
        driver = new ChromeDriver(chromeOptions());
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
    }

    /**
     * 用 TestWatcher 接管浏览器收尾：失败先截图再退出，避免 @AfterEach 提前 quit 导致截不到图。
     */
    @RegisterExtension
    final TestWatcher browserLifecycle = new TestWatcher() {
        @Override
        public void testSuccessful(ExtensionContext context) {
            quitQuietly();
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            captureScreenshot(context.getDisplayName());
            quitQuietly();
        }

        @Override
        public void testAborted(ExtensionContext context, Throwable cause) {
            quitQuietly();
        }

        @Override
        public void testDisabled(ExtensionContext context, Optional<String> reason) {
            quitQuietly();
        }
    };

    // ---------- 环境信息 ----------

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /** 打开相对路径并等待 URL 落到期望位置 */
    protected void openAndWaitUrl(String path, String urlFragment) {
        driver.get(baseUrl() + path);
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /** 等待当前 URL 落到期望位置（不触发导航） */
    protected void waitForUrlContains(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    // ---------- 页面入口 ----------

    protected LoginPage loginPage() {
        return new LoginPage(driver, wait, baseUrl());
    }

    protected Directions2030Page directions2030Page() {
        return new Directions2030Page(driver, wait, baseUrl());
    }

    /** 打开登录页并登录，返回菜单页 */
    protected MenuPage loginAs(String userId, String password) {
        return loginPage().open().loginAs(userId, password);
    }

    /** 以管理员身份登录并直接进入通知邮件管理画面 */
    protected Directions2030Page loginAndOpenDirections2030(String surveyType) {
        loginAs(UITestData.USER_ADMIN, UITestData.PASSWORD);
        return directions2030Page().open(surveyType);
    }

    // ---------- 浏览器配置 ----------

    private ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        // selenium.headless 默认 true；传 -Dselenium.headless=false 可开启有头模式调试
        if (!"false".equalsIgnoreCase(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1440,900"
        );

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", DOWNLOAD_DIR.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    // ---------- 下载辅助 ----------

    /** 清空下载目录（在触发下载前调用，避免拿到上一轮的残留文件） */
    protected void clearDownloadDir() {
        try {
            Files.createDirectories(DOWNLOAD_DIR);
            try (var files = Files.list(DOWNLOAD_DIR)) {
                for (Path path : files.toList()) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException ignored) {
            // 清理失败不阻断用例
        }
    }

    /** 等待文件落到下载目录并返回其路径 */
    protected Path waitForDownload(String fileName) {
        return wait.until(d -> {
            Path target = DOWNLOAD_DIR.resolve(fileName);
            return Files.isRegularFile(target) ? target : null;
        });
    }

    // ---------- 收尾辅助 ----------

    private void captureScreenshot(String testName) {
        if (!(driver instanceof TakesScreenshot screenshot)) {
            return;
        }
        try {
            Path dir = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(dir);
            Files.write(dir.resolve(safeFileName(testName) + ".png"),
                    screenshot.getScreenshotAs(OutputType.BYTES));
        } catch (IOException | RuntimeException ignored) {
            // 截图属于诊断辅助，失败不影响测试结论
        }
    }

    private void quitQuietly() {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (RuntimeException ignored) {
            // 浏览器可能已自行退出
        } finally {
            driver = null;
        }
    }

    private String safeFileName(String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5_-]", "_");
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }
}
