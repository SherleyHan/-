-- 重置测试数据：每个测试前调用
DELETE FROM directions_execution WHERE execution_id != 'TEST_WAIT_001';
DELETE FROM notification_mail WHERE survey_type != 'SY' OR id > 10;
