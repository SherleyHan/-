package jp.go.mext.selenium.data;

/**
 * UI 自动化测试数据常量。
 *
 * <p>集中管理测试账号、路由与提示文案，避免散落在各测试类里。
 * 账号与密码必须与 {@code src/main/resources/db/data-test.sql} 保持一致。
 */
public final class UITestData {

    private UITestData() {
    }

    // ---------- 测试账号（对应 data-test.sql） ----------

    /** 系统管理员，調査类别 SY，可正常访问通知邮件管理 */
    public static final String USER_ADMIN = "test_admin";

    /** 一般用户，調査类别 SY */
    public static final String USER_NORMAL = "test_user";

    /** 已锁定账号（account_locked = TRUE） */
    public static final String USER_LOCKED = "test_locked";

    /** 库中不存在的用户，用于校验错误提示 */
    public static final String USER_UNKNOWN = "no_such_user";

    /** data-test.sql 中密码哈希对应的明文密码 */
    public static final String PASSWORD = "password123";

    // ---------- 路由 ----------

    public static final String PATH_LOGIN = "/login";
    public static final String PATH_LOGIN_FORM = "/loginZ000";
    public static final String PATH_MENU = "/menu";
    public static final String PATH_CONFIRM = "/directions2030/detail/modify";
    public static final String PATH_DIRECTIONS_PREFIX = "/directions2030/detail/";

    /** 通知邮件管理 - 調査类别 SY */
    public static final String SURVEY_TYPE_SY = "SY";
    /** 通知邮件管理 - 調査类别 HI */
    public static final String SURVEY_TYPE_HI = "HI";

    // ---------- 页面元素 ID ----------

    public static final String ID_LOGIN_FORM = "loginForm";
    public static final String ID_USER_ID = "userId";
    public static final String ID_PASSWORD = "password";
    public static final String ID_CONFIRM_FORM = "confirmForm";

    // ---------- 前端校验 / 服务端提示文案 ----------

    /** 登录页：用户ID 含非法字符（loginZ000.js） */
    public static final String MSG_LOGIN_ILLEGAL_USER_ID = "不正な文字";

    /** 登录页：服务端返回的通用失败提示（LoginService） */
    public static final String MSG_LOGIN_FAILED = "错误";

    /** 登录页：账号锁定提示 */
    public static final String MSG_ACCOUNT_LOCKED = "锁定";

    /** 通知邮件管理：邮箱为空（directions2030.js） */
    public static final String MSG_MAIL_EMPTY = "メールアドレスを入力してください";

    /** 通知邮件管理：邮箱格式不合法（directions2030.js） */
    public static final String MSG_MAIL_INVALID = "正しくありません";

    /** 通知邮件管理：未选择文件（directions2030.js） */
    public static final String MSG_FILE_REQUIRED = "ファイルを選択してください";

    /** 通知邮件管理：文件名含非法字符（directions2030.js） */
    public static final String MSG_FILE_NAME_INVALID = "不正な文字";

    /** 通知邮件管理：扩展名非 .txt（directions2030.js） */
    public static final String MSG_FILE_EXT_INVALID = "拡張子は .txt";

    /** 反映成功横幅（directions2030.jsp） */
    public static final String MSG_REFLECTED = "画面に反映しました";

    /** 执行成功（Directions2030Service / commonConfirm.jsp） */
    public static final String MSG_EXECUTE_SUCCESS = "执行成功";

    /** 执行成功后展示的指示 ID 前缀 */
    public static final String MSG_EXECUTE_ORDER_ID = "指示ID";

    /** CSS class：提示为错误态 */
    public static final String CSS_ERROR = "error";

    /** CSS class：提示为信息态 */
    public static final String CSS_INFO = "info";

    // ---------- 测试用邮箱数据 ----------

    public static final String MAIL_A = "selenium-a@example.com";
    public static final String MAIL_B = "selenium-b@example.com";
    public static final String MAIL_INVALID = "not-an-email";
    public static final String MAIL_SEPARATOR = ":";
}
