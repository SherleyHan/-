<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>${form.gamenName}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<div class="page-container">
    <h1>${form.gamenName}</h1>
    <p>画面ID: ${form.gamenId} / 調査类别: ${form.gamenSyubetu}</p>

    <c:if test="${reflected}">
        <div class="info">ファイルの内容を画面に反映しました。内容を確認して「変更」ボタンを押してください。</div>
    </c:if>

    <form id="form2030" method="post" enctype="multipart/form-data">
        <input type="hidden" name="gamenSyubetu" value="${form.gamenSyubetu}">

        <h2>一括登记ファイルアップロード</h2>
        <p>文件名規約: ${form.originalFileName}（拡張子 .${form.fileExt}）</p>
        <input type="file" id="uploadFile" name="uploadFile">
        <button type="button" id="btnReflect">反映</button>
        <button type="button" id="btnDownload">ファイルの圧縮ダウンロード</button>

        <h2>通知メール一覧</h2>
        <table border="1">
            <thead>
                <tr><th>No.</th><th>通知对象</th><th>メールアドレス</th></tr>
            </thead>
            <tbody>
                <c:forEach var="mail" items="${form.mailList}" varStatus="st">
                    <tr>
                        <td>${st.count}</td>
                        <td>通知对象${st.count}</td>
                        <td>${mail}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <h2>編集</h2>
        <p>複数のアドレスは半角コロン（:）で区切ってください。</p>
        <textarea id="mailAddressEdit" name="mailAddressEdit" rows="3" cols="60">${form.mailAddressEdit}</textarea>

        <div class="form-row">
            <button type="button" id="btnModify">変更</button>
            <button type="button" id="btnExecute">実行</button>
            <button type="button" onclick="history.back()">戻る</button>
        </div>
    </form>

    <div id="message" class="message"></div>
</div>

<script src="/js/directions2030.js"></script>
<script>
    // 把服务端初始化的 form 状态传到前端
    window.MEXT_FORM = {
        gamenSyubetu: '${form.gamenSyubetu}',
        mailList: [
            <c:forEach var="mail" items="${form.mailList}" varStatus="st">
                '${mail}'<c:if test="${not st.last}">,</c:if>
            </c:forEach>
        ]
    };
</script>
</body>
</html>
