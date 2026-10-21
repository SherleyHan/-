-- ============================================================
-- UI 自动化测试前置重置脚本
-- 每个 UI 用例执行前运行，保证用例可重复执行且互不影响。
-- 背景：LoginService.authenticate 密码错误会累加 failed_count，
--       达到 mext.login.max-retry(5) 后会把账号锁定，
--       若不重置，同一个 JVM 内多次错误登录会污染后续用例。
-- ============================================================

-- 1) 登录账号：清空失败计数，仅保留 test_locked 为锁定态
UPDATE m_user SET failed_count = 0, account_locked = FALSE;
UPDATE m_user SET account_locked = TRUE WHERE user_id = 'test_locked';

-- 2) 指示记录：保留基线记录，清理用例产生的执行记录
DELETE FROM directions_execution WHERE execution_id <> 'TEST_WAIT_001';

-- 3) 通知邮件：恢复为唯一初始记录（1 条 SY），避免行数断言漂移
DELETE FROM notification_mail;
INSERT INTO notification_mail (survey_type, mail_address, file_name, file_ext, update_user)
VALUES ('SY', 'a@example.com:b@example.com', 'mail_to_new.txt', 'txt', 'test_admin');
