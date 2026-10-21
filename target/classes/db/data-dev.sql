-- 初始化数据：dev 环境
-- 密码全部为 "password123" 经过 SHA-256 + salt "mext2026" 后的值
INSERT INTO m_user (user_id, user_name, password, riyousha_cd, research_type, authority_cd)
VALUES
    ('admin',     '系统管理员', '623eac84e468c7a0e4817c3100d8ac8cb38d2a67e64e3c8d2280142ba3fd3ea4', 'A', 'SY', 'ADM'),
    ('test_user', '测试用户',   '623eac84e468c7a0e4817c3100d8ac8cb38d2a67e64e3c8d2280142ba3fd3ea4', 'U', 'SY', 'USR'),
    ('no_auth',   '无权限用户', '623eac84e468c7a0e4817c3100d8ac8cb38d2a67e64e3c8d2280142ba3fd3ea4', 'U', 'XX', 'USR');

INSERT INTO notification_mail (survey_type, mail_address, file_name, file_ext, update_user) VALUES
    ('SY', 'admin@example.com:test@example.com', 'mail_to_new.txt', 'txt', 'admin'),
    ('HI', 'hi-team@example.com', 'mail_to_new.txt', 'txt', 'admin'),
    ('HL', 'hl-team@example.com', 'mail_to_new.txt', 'txt', 'admin');

INSERT INTO directions_execution (execution_id, survey_type, business_type, status, mail_address) VALUES
    ('TEST001', 'SY', 'NOTIFY_MAIL', 'END',  'admin@example.com'),
    ('TEST002', 'SY', 'NOTIFY_MAIL', 'WAIT', 'pending@example.com'),
    ('TEST003', 'HI', 'NOTIFY_MAIL', 'RUN',  'running@example.com');
