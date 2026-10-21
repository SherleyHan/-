/**
 * directions2030.js - 通知邮件管理画面交互
 */
(function () {
    'use strict';

    const messageEl = document.getElementById('message');

    function showMessage(text, isError) {
        messageEl.textContent = text;
        messageEl.className = 'message ' + (isError ? 'error' : 'info');
    }

    // ---------- 文件名校验 ----------
    const FILE_NAME_PATTERN = /^[A-Za-z0-9_.-]+$/;

    function validateUploadFile(file) {
        if (!file) return 'ファイルを選択してください';
        if (!FILE_NAME_PATTERN.test(file.name)) return 'ファイル名に不正な文字が含まれています';
        if (!file.name.endsWith('.txt')) return '拡張子は .txt である必要があります';
        return null;
    }

    // ---------- 邮箱校验 ----------
    const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;

    function validateEmails(text) {
        if (!text || !text.trim()) return 'メールアドレスを入力してください';
        const mails = text.split(':').map(s => s.trim()).filter(Boolean);
        if (mails.length === 0) return '少なくとも一つのメールアドレスが必要です';
        for (const m of mails) {
            if (!EMAIL_PATTERN.test(m)) return 'メールアドレスの形式が正しくありません: ' + m;
        }
        return null;
    }

    // ---------- 反映按钮 ----------
    document.getElementById('btnReflect').addEventListener('click', function () {
        const fileInput = document.getElementById('uploadFile');
        const file = fileInput.files[0];
        const err = validateUploadFile(file);
        if (err) { showMessage(err, true); return; }

        const formData = new FormData(document.getElementById('form2030'));
        showMessage('アップロード中...', false);

        fetch('/directions2030/detail/reflect', {
            method: 'POST',
            body: formData
        })
        .then(r => {
            if (r.redirected) { window.location.href = r.url; return null; }
            return r.text();
        })
        .then(html => {
            if (html) {
                document.open();
                document.write(html);
                document.close();
            }
        })
        .catch(err => showMessage('システムエラー: ' + err.message, true));
    });

    // ---------- 下载按钮 ----------
    document.getElementById('btnDownload').addEventListener('click', function () {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/directions2030/detail/compressionDownload';
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'gamenSyubetu';
        input.value = window.MEXT_FORM.gamenSyubetu;
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    });

    // ---------- 变更按钮 ----------
    document.getElementById('btnModify').addEventListener('click', function () {
        const text = document.getElementById('mailAddressEdit').value;
        const err = validateEmails(text);
        if (err) { showMessage(err, true); return; }

        document.getElementById('form2030').action = '/directions2030/detail/modify';
        document.getElementById('form2030').submit();
    });

    // ---------- 执行按钮（AJAX） ----------
    document.getElementById('btnExecute').addEventListener('click', function () {
        const text = document.getElementById('mailAddressEdit').value;
        const err = validateEmails(text);
        if (err) { showMessage(err, true); return; }

        const mails = text.split(':').map(s => s.trim()).filter(Boolean);
        const body = new URLSearchParams();
        body.append('gamenSyubetu', window.MEXT_FORM.gamenSyubetu);
        mails.forEach(m => body.append('mailList', m));

        showMessage('実行中...', false);

        fetch('/directions2030/detail/execute', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        })
        .then(r => r.json())
        .then(res => {
            if (res.statusCode === '0') {
                showMessage(res.message + ' - 3秒後にメニューに戻ります', false);
                setTimeout(() => window.location.href = '/menu', 3000);
            } else {
                showMessage(res.message || '実行失敗', true);
            }
        })
        .catch(err => showMessage('システムエラー: ' + err.message, true));
    });
})();
