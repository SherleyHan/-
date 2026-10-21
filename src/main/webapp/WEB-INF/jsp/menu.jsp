<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>メニュー - 文部科学省 調査統計業務システム</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<div class="menu-container">
    <h1>メインメニュー</h1>
    <p>ようこそ、${sessionScope.userSession.userName} さん</p>
    <ul class="menu-list">
        <li><a href="/directions2030/detail/SY">通知メール管理（SY）</a></li>
        <li><a href="/directions2030/detail/HI">通知メール管理（HI）</a></li>
    </ul>
    <div class="menu-actions" style="margin-top: 20px;">
        <button type="button" onclick="location.href='/directions2030/detail/SY'">通知邮件管理页</button>
    </div>
    <form method="post" action="/logout" style="margin-top: 30px;">
        <button type="submit">ログアウト</button>
    </form>
</div>
</body>
</html>
