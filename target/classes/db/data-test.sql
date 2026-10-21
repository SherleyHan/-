-- 测试数据：每个测试方法独立可见（数据库在内存中重新初始化）
-- 密码 password123 + salt "mext2026" 的 SHA-256 哈希
INSERT INTO m_user (user_id, user_name, password, riyousha_cd, research_type, authority_cd) VALUES
    ('test_admin', 'Test Admin', '623eac84e468c7a0e4817c3100d8ac8cb38d2a67e64e3c8d2280142ba3fd3ea4', 'A', 'SY', 'ADM'),
    ('test_user',  'Test User',  '623eac84e468c7a0e4817c3100d8ac8cb38d2a67e64e3c8d2280142ba3fd3ea4', 'U', 'SY', 'USR'),
    ('test_locked','Locked User','623eac84e468c7a0e4817c3100d8ac8cb38d2a67e64e3c8d2280142ba3fd3ea4', 'U', 'SY', 'USR');

UPDATE m_user SET account_locked = TRUE WHERE user_id = 'test_locked';

INSERT INTO notification_mail (survey_type, mail_address, file_name, file_ext, update_user) VALUES
    ('SY', 'a@example.com:b@example.com', 'mail_to_new.txt', 'txt', 'test_admin');

INSERT INTO directions_execution (execution_id, survey_type, business_type, status, mail_address) VALUES
    ('TEST_WAIT_001', 'SY', 'NOTIFY_MAIL', 'WAIT', 'a@example.com');
