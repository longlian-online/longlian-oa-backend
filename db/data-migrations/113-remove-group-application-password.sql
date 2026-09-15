-- 生产库的 group_application.password 由旧版本创建，先清理历史凭证再删除字段。
-- 请在 db/schema.sql 更新后人工审核执行；Atlas 生产环境会跳过字段删除。
UPDATE `group_application`
SET `password` = NULL
WHERE `password` IS NOT NULL;

ALTER TABLE `group_application`
    DROP COLUMN `password`;
