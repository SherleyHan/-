<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>ログイン - 文部科学省 調査統計業務システム</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<div class="login-container">
    <h1>ログイン</h1>

    <c:if test="${not empty errorMessage}">
        <div class="error">${errorMessage}</div>
    </c:if>

    <form id="loginForm" method="post" action="/login">
        <div class="form-row">
            <label for="userId">ユーザーID</label>
            <input type="text" id="userId" name="userId" maxlength="20" required>
        </div>
        <div class="form-row">
            <label for="password">パスワード</label>
            <input type="password" id="password" name="password" maxlength="50" required>
        </div>
        <div class="form-row">
            <button type="submit" id="btnLogin">ログイン</button>
        </div>
    </form>

    <div id="message" class="message"></div>

    <div class="hint">
        <p>テスト用アカウント：</p>
        <ul>
            <li>admin / password123 (システム管理者、調査类型 SY)</li>
            <li>test_user / password123 (一般ユーザー、調査类型 SY)</li>
            <li>no_auth / password123 (権限なし)</li>
            <li>666</li>
        </ul>
    </div>
</div>

<script src="/js/loginZ000.js"></script>
</body>
</html>
