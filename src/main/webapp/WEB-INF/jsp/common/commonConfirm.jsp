<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>指示内容確認 - ${form.gamenName}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<div class="page-container">
    <h1>指示内容確認</h1>
    <p>画面ID: ${form.gamenId} / 調査类别: ${form.gamenSyubetu}</p>

    <h2>変更後のメールアドレス</h2>
    <table border="1">
        <thead>
            <tr><th>No.</th><th>メールアドレス</th></tr>
        </thead>
        <tbody>
            <c:forEach var="mail" items="${form.mailList}" varStatus="st">
                <tr>
                    <td>${st.count}</td>
                    <td>${mail}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <form id="confirmForm" method="post">
        <input type="hidden" name="gamenSyubetu" value="${form.gamenSyubetu}">
        <c:forEach var="mail" items="${form.mailList}">
            <input type="hidden" name="mailList" value="${mail}">
        </c:forEach>

        <div class="form-row" style="margin-top: 20px;">
            <button type="button" id="btnConfirmExecute">実行</button>
            <button type="button" onclick="history.back()">戻る</button>
        </div>
    </form>

    <div id="message" class="message"></div>
</div>

<script>
    const messageEl = document.getElementById('message');
    function showMessage(text, isError) {
        messageEl.textContent = text;
        messageEl.className = 'message ' + (isError ? 'error' : 'info');
    }

    document.getElementById('btnConfirmExecute').addEventListener('click', function () {
        if (!confirm('本当に実行しますか？')) return;

        const form = document.getElementById('confirmForm');
        const body = new URLSearchParams(new FormData(form));
        showMessage('実行中...', false);

        fetch('/directions2030/detail/execute', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        })
        .then(r => r.json())
        .then(res => {
            if (res.statusCode === '0') {
                showMessage(res.message + ' - メニューに戻ります', false);
                setTimeout(() => window.location.href = '/menu', 3000);
            } else {
                showMessage(res.message || '実行失敗', true);
            }
        })
        .catch(err => showMessage('システムエラー: ' + err.message, true));
    });
</script>
</body>
</html>
