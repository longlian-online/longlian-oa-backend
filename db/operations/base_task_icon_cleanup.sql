-- 一次性、破坏性的发布清理，不由 migrate.sh 自动执行。
-- 仅在完成自动数据保留迁移、核对图标数据并部署新应用后，经审核显式执行。
-- 生产 Atlas 保留旧列；旧应用回滚窗口结束前不要执行此清理。
SET @has_base_task = (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_task');
SET @sql = IF(@has_base_task = 1,
    'ALTER TABLE base_task DROP COLUMN IF EXISTS icon_name, DROP COLUMN IF EXISTS icon_file_id',
    'SELECT 1');
PREPARE icon_release_statement FROM @sql;
EXECUTE icon_release_statement;
DEALLOCATE PREPARE icon_release_statement;
