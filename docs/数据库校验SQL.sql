-- ============================================================
-- 数据库校验 SQL
-- 用于人工或在集成测试中验证业务状态
-- ============================================================

-- 1. 用户表基础校验
SELECT user_id, user_name, authority_cd, account_locked, failed_count, password_expired
FROM m_user
WHERE delete_flag = FALSE
ORDER BY user_id;

-- 2. 通知邮件地址按调查类别聚合
SELECT survey_type,
       COUNT(*) AS mail_count,
       STRING_AGG(mail_address, ';' ORDER BY id) AS all_mails
FROM notification_mail
GROUP BY survey_type
ORDER BY survey_type;

-- 3. 单调查类别邮件地址详情
SELECT id, survey_type, mail_address, file_name, update_user, update_date
FROM notification_mail
WHERE survey_type = 'SY'
ORDER BY id;

-- 4. 指示执行状态分布
SELECT status, COUNT(*) AS cnt
FROM directions_execution
GROUP BY status
ORDER BY status;

-- 5. WAIT 指示详情
SELECT execution_id, survey_type, business_type, mail_address,
       file_path, create_user, create_date
FROM directions_execution
WHERE status = 'WAIT'
ORDER BY create_date;

-- 6. 异常 (ABEND) 指示及其原因
SELECT execution_id, survey_type, business_type,
       error_message, start_time, end_time
FROM directions_execution
WHERE status = 'ABEND'
ORDER BY end_time DESC;

-- 7. 最近 N 条 END 指示
SELECT execution_id, survey_type, business_type,
       start_time, end_time,
       EXTRACT(EPOCH FROM (end_time - start_time)) AS cost_seconds
FROM directions_execution
WHERE status = 'END'
ORDER BY end_time DESC
LIMIT 20;

-- 8. 用户连续登录失败但未锁定（潜在风险）
SELECT user_id, user_name, failed_count, account_locked
FROM m_user
WHERE failed_count > 0 AND account_locked = FALSE
ORDER BY failed_count DESC;

-- 9. 邮件发送历史（生产环境）
SELECT mail_subject, mail_to, status, send_time
FROM mail_send_history
ORDER BY send_time DESC
LIMIT 50;

-- 10. 验证文件路径是否存在（PostgreSQL）
SELECT execution_id, file_path,
       CASE WHEN file_path IS NULL THEN 'NO_PATH'
            ELSE 'HAS_PATH' END AS path_status
FROM directions_execution
WHERE status IN ('WAIT', 'RUN', 'END')
ORDER BY update_date DESC;
