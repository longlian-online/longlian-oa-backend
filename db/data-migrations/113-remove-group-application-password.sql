-- 注册申请的密码哈希已迁移到 user.password，清理申请表中的历史凭证后删除字段。
UPDATE group_application
SET password = NULL
WHERE password IS NOT NULL;

ALTER TABLE group_application
    DROP COLUMN password;
