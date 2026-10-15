-- 一次性、破坏性的发布清理，不由 migrate.sh 自动执行。
-- Atlas 的 dev/prod 都会保留旧列。部署不再读写 item.status 的应用后，经审核显式执行。
SET @has_item_status = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item' AND COLUMN_NAME = 'status');
SET @sql = IF(@has_item_status = 1,
    'ALTER TABLE item DROP COLUMN status',
    'SELECT 1');
PREPARE item_status_cleanup FROM @sql;
EXECUTE item_status_cleanup;
DEALLOCATE PREPARE item_status_cleanup;
