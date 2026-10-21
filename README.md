# 文部科学省 調査統計業務システム (mext-survey-system)

基于 Spring Boot 3 + JSP + MyBatis + PostgreSQL 的调查统计业务项目，覆盖 Web UI 自动化、接口测试和后端逻辑测试三层。

## 📋 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [Maven Profile](#maven-profile)
- [测试运行](#测试运行)
- [API 列表](#api-列表)
- [数据库校验](#数据库校验)

## 技术栈

| 层级 | 技术 |
|------|------|
| 容器 | Spring Boot 3.2 + 内嵌 Tomcat（支持 JSP）|
| 持久化 | MyBatis 3 + PostgreSQL / H2 (测试) |
| 视图 | JSP + JSTL + Tomcat-embed-jasper |
| 测试 | JUnit 5 + Mockito + Spring Test + Selenium WebDriver |
| 日志 | Log4j2 |
| 构建 | Maven 3.9 + Java 17 |

## 项目结构

```
mext-survey-system/
├── pom.xml                          # Maven 配置（含 dev/test/pg/selenium 4 个 profile）
├── README.md                        # 本文档
├── src/main/java/jp/go/mext/
│   ├── MextSurveyApplication.java   # Web 主应用入口
│   ├── batch/                       # 批处理模块
│   │   ├── BatchAplApplication.java # 批处理入口（java -jar mext-survey-system.jar batch <ID>）
│   │   ├── BT_MA010101.java        # WAIT 指示处理：mail_to_new.txt → mail_to.txt
│   │   └── BT_CM000048.java        # 邮件发送
│   ├── config/WebMvcConfig.java    # 拦截器配置
│   ├── controller/
│   │   ├── IndexController.java    # 首页跳转
│   │   ├── LoginController.java    # 登录/登出
│   │   └── Directions2030Controller.java # 通知邮件管理
│   ├── service/
│   │   ├── LoginService.java       # 登录业务（含 SHA-256 密码哈希、失败次数）
│   │   └── Directions2030Service.java # 通知邮件管理业务
│   ├── dao/                         # MyBatis @Mapper 接口
│   ├── dto/                         # 业务 DTO + Form + Session
│   ├── enums/
│   │   ├── ResultStatus.java       # RESULT_OK / RESULT_ERROR / CHECK_ERROR / SESSION_MUKOU
│   │   └── BatchStatus.java        # WAIT / RUN / END / ABEND
│   ├── exception/                   # 业务异常 + 全局异常处理
│   ├── interceptor/
│   │   ├── LoginInterceptor.java
│   │   └── ApplicationLogInterceptor.java
│   └── util/PasswordEncoder.java   # SHA-256 + salt 加密工具
├── src/main/resources/
│   ├── application*.properties     # dev/test/pg 三套配置
│   ├── db/
│   │   ├── schema.sql               # 5 张表：m_user / notification_mail / directions_execution / file_status / mail_send_history
│   │   ├── data-dev.sql
│   │   └── data-test.sql
│   ├── mybatis/*.xml                # MyBatis SQL 映射
│   └── log4j2.xml
├── src/main/webapp/
│   ├── WEB-INF/jsp/                 # JSP 页面
│   │   ├── menu.jsp
│   │   ├── login/loginZ000.jsp
│   │   ├── directions2030/directions2030.jsp
│   │   └── common/commonConfirm.jsp
│   ├── css/style.css
│   └── js/                          # 前端校验
├── src/test/java/...                # 73 个测试（详见下面）
├── src/test/resources/db/reset-survey-test.sql
├── postman/MextSurvey.postman_collection.json  # Postman 接口测试集合
└── docs/
    ├── 测试用例.md                  # 测试用例说明
    └── 数据库校验SQL.sql           # 人工校验 SQL
```

## 环境要求

- **JDK 17** （macOS brew 用户：`brew install openjdk@17`）
- **Maven 3.9+**（`brew install maven`）
- **PostgreSQL 14+**（可选：dev/test profile 用 H2 内存库，无需安装）

## 快速开始

### 1. 启动开发环境（默认 H2，无外部依赖）

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

cd /Users/sherlly/Documents/workbuddy/mext-survey-system
mvn spring-boot:run
```

浏览器打开 <http://localhost:8080/login>

**测试账号**（data-dev.sql）：
| userId   | password    | 角色 |
|----------|-------------|------|
| admin    | password123 | 系统管理员（SY 调查类别权限）|
| test_user| password123 | 普通用户（SY 权限）|
| no_auth  | password123 | 无调查权限（演示 SESSION_MUKOU）|

### 2. 启动生产环境（真实 PostgreSQL）

```bash
# 1. 建库
psql -U postgres -c "CREATE DATABASE mext_survey;"
psql -U postgres -d mext_survey -c "CREATE USER mext WITH PASSWORD 'please_change_me';"
psql -U postgres -d mext_survey -c "GRANT ALL ON DATABASE mext_survey TO mext;"

# 2. 启动
mvn spring-boot:run -Ppg
```

## Maven Profile

| Profile | 默认 | 用途 | 数据库 |
|---------|------|------|--------|
| `dev`   | ✅   | 本地开发 | H2 内存（embedded）|
| `test`  |      | 测试运行 | H2 内存（embedded）|
| `pg`    |      | 生产 | PostgreSQL |
| `selenium` |   | 启用 `@Tag("ui")` 的 Selenium UI 测试 | 独立 H2 库 |

切换：
```bash
mvn test -Pselenium     # 含 Selenium UI 测试
mvn spring-boot:run -Ppg  # 切 PostgreSQL
```

## 测试运行

### 跑全部测试（73 个，约 3 秒）

```bash
mvn test
```

> UI 用例带 `@Tag("ui")`，默认被 surefire `excludedGroups` 排除，不拖慢日常构建。

### 跑指定测试类

```bash
mvn -Dtest=LoginServiceTest test
mvn -Dtest=Directions2030ServiceTest test
mvn -Dtest=SurveyFlowIntegrationTest test
```

### 包含 Selenium UI 自动化（全量 98 个，约 40 秒；需要 Chrome）

```bash
mvn test -Pselenium
```

只跑某个 UI 类：
```bash
mvn test -Pselenium -Dtest=LoginUITest
```

或带 GUI 调试：
```bash
mvn test -Pselenium -Dselenium.headless=false
```

> ChromeDriver 由 WebDriverManager 自动匹配，无需手工安装。
> 失败会自动截图到 `target/ui-screenshots/`。

### 测试分层概览

| 测试类 | 数量 | 测试类型 | 验证点 |
|--------|------|----------|--------|
| BatchStatusTest | 7 | 纯单元 | 状态机转换规则 |
| PasswordEncoderTest | 4 | 纯单元 | SHA-256 哈希 |
| ValidationUtilsTest | 13 | 纯单元 | 邮箱/文件名/调查类别校验 |
| LoginServiceTest | 8 | Mockito 单元 | 登录/锁定/密码过期/失败计数 |
| Directions2030ServiceTest | 11 | Mockito 单元 | 编辑/上传/执行指示 |
| BT_MA010101Test | 5 | Mockito 单元 | 批处理状态流转 + 文件操作 |
| LoginControllerTest | 5 | @WebMvcTest | Controller 路由 + 状态码 |
| Directions2030ControllerTest | 5 | @WebMvcTest | Controller 路由 + 状态码 |
| DirectionsExecutionDaoIT | 6 | @SpringBootTest 集成 | DAO 真实 SQL 执行 |
| NotificationMailDaoIT | 6 | @SpringBootTest 集成 | DAO 真实 SQL 执行 |
| SurveyFlowIntegrationTest | 3 | @SpringBootTest 集成 | 登录→编辑→执行→数据库全链路 |
| **LoginUITest** | **7** | **Selenium UI** | **登录必填/非法字符/密码错误/锁定/成功跳转** |
| **MenuUITest** | **4** | **Selenium UI** | **菜单渲染/入口跳转/注销/未登录拦截** |
| **Directions2030UITest** | **10** | **Selenium UI** | **邮件列表/文件反映/邮箱与文件校验/执行/下载** |
| **ConfirmUITest** | **4** | **Selenium UI** | **确认页回显/返回/执行与取消确认框** |

UI 层采用 Page Object Model：`selenium/base`（基类）、`selenium/pages`（4 个页面对象）、`selenium/data`（测试数据）。

## API 列表

### 登录/登出

| Method | URL | 说明 |
|--------|-----|------|
| GET  | /login              | 登录页 |
| POST | /login              | 校验登录（返回 JSON `{statusCode, userName}`）|
| POST | /loginConfirm       | 写入 session 后跳转 menu |
| POST | /logout             | 销毁 session |

### 通知邮件管理

| Method | URL | 说明 |
|--------|-----|------|
| POST | /directions2030/detail/{gamenSyubetu} | 打开编辑画面（SY/HI/HL/...）|
| POST | /directions2030/detail/reflect    | 上传文件反映到页面 |
| POST | /directions2030/detail/modify     | 编辑邮件地址 → 确认页 |
| POST | /directions2030/detail/execute    | 执行指示（AJAX，返回 JSON）|
| POST | /directions2030/detail/compressionDownload | 压缩下载 |

### 业务状态码

| 状态码 | 含义 | 触发场景 |
|--------|------|----------|
| `0`    | RESULT_OK | 操作成功 |
| `-1`   | RESULT_ERROR | 业务失败 |
| `1`    | CHECK_ERROR | 参数校验失败 |
| `90`   | SESSION_MUKOU | 会话失效/无权限 |
| `-99`  | SYSTEM_ERROR | 系统异常 |

### Postman 集合

`postman/MextSurvey.postman_collection.json` 直接导入 Postman 即可，包含：
- 登录正常/异常用例
- 通知邮件管理 CRUD
- 数据库校验 SQL 说明

## 数据库校验

执行 `docs/数据库校验SQL.sql` 中的 SQL 可验证：
- 用户/通知邮件/批处理指示状态
- 失败重试账号（潜在风险）
- ABEND 异常指示
- 文件生成路径

## 批处理运行

```bash
# 打包
mvn package -DskipTests

# 处理 WAIT 指示（mail_to_new.txt → mail_to.txt）
java -jar target/mext-survey-system.jar batch BT_MA010101 /var/mext/mail

# 发送通知邮件
java -jar target/mext-survey-system.jar batch BT_CM000048 SY "件名" "本文"
```

## 面试使用提示

这份代码可以直接映射到简历里提到的所有场景：

- ✅ **Web 自动化（Selenium + Page Object Model）** → `selenium/base/`、`selenium/pages/`、`selenium/*UITest.java`（4 类 25 用例，覆盖登录跳转、必填/邮箱/文件校验及错误提示）
- ✅ **接口测试（Postman）** → `postman/MextSurvey.postman_collection.json`
- ✅ **JUnit 5 + Mockito 单元测试** → `service/LoginServiceTest.java` 等
- ✅ **Spring Test 集成测试** → `integration/SurveyFlowIntegrationTest.java`
- ✅ **批处理状态流转（WAIT/RUN/END/ABEND）** → `batch/BT_MA010101Test.java`
- ✅ **数据库一致性验证** → `dao/*DaoIT.java` + `docs/数据库校验SQL.sql`
- ✅ **业务状态码（RESULT_OK/RESULT_ERROR/CHECK_ERROR/SESSION_MUKOU）** → `enums/ResultStatus.java`
- ✅ **登录校验、失败计数、账号锁定** → `service/LoginService.java` + 对应测试
- ✅ **文件上传/名校验/格式校验** → `service/Directions2030Service.java`
