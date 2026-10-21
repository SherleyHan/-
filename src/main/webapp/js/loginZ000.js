/**
 * loginZ000.js - 登录页前端校验
 */
(function () {
    'use strict';

    const form = document.getElementById('loginForm');
    const userIdEl = document.getElementById('userId');
    const passwordEl = document.getElementById('password');
    const messageEl = document.getElementById('message');

    // 非法字符正则：仅允许字母数字下划线短横线
    const ILLEGAL_CHARS = /[^A-Za-z0-9_-]/;

    function showMessage(text, isError) {
        messageEl.textContent = text;
        messageEl.className = 'message ' + (isError ? 'error' : 'info');
    }

    function validate() {
        const userId = userIdEl.value.trim();
        const password = passwordEl.value;

        if (!userId) {
            showMessage('ユーザーIDを入力してください', true);
            userIdEl.focus();
            return null;
        }
        if (ILLEGAL_CHARS.test(userId)) {
            showMessage('ユーザーIDに不正な文字が含まれています', true);
            userIdEl.focus();
            return null;
        }
        if (!password) {
            showMessage('パスワードを入力してください', true);
            passwordEl.focus();
            return null;
        }
        return { userId: userId, password: password };
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const data = validate();
        if (!data) return;

        showMessage('ログイン中...', false);

        fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(data)
        })
        .then(r => r.json())
        .then(res => {
            if (res.statusCode === '0') {
                // 成功 → 跳转 confirm
                return fetch('/loginConfirm', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(data)
                });
            } else if (res.requireConfirm) {
                if (confirm(res.message)) {
                    // 用户确认 → 直接 confirm
                    return fetch('/loginConfirm', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: new URLSearchParams(data)
                    });
                }
                return null;
            } else {
                showMessage(res.message || 'ログイン失敗', true);
                return null;
            }
        })
        .then(r => r ? r.json() : null)
        .then(res => {
            if (res && res.statusCode === '0') {
                window.location.href = res.redirect || '/menu';
            }
        })
        .catch(err => showMessage('システムエラー: ' + err.message, true));
    });
})();
