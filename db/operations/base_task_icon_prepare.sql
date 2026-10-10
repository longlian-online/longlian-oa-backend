-- 一次性发布操作，不由 migrate.sh 自动执行。
-- 先备份并停止旧应用写入，再选择目标数据库执行本文件；随后审核 Atlas plan 并 apply。
-- 新库无需预处理；旧库新增目标列而不删除来源列，以便先核对数据再部署新应用。
SET @has_base_task = (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_task');
SET @sql = IF(@has_base_task = 1,
    'ALTER TABLE base_task ADD COLUMN IF NOT EXISTS icon varchar(100) NULL COMMENT ''Lucide 图标组件名'' AFTER name',
    'SELECT 1');
PREPARE icon_release_statement FROM @sql;
EXECUTE icon_release_statement;
DEALLOCATE PREPARE icon_release_statement;

SET @has_icon_name = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_task' AND COLUMN_NAME = 'icon_name');
-- 保留已有目标值；重复执行或新增目标列后中断，都可以重新执行。
SET @sql = IF(@has_icon_name = 1,
    'UPDATE base_task SET icon = NULLIF(icon_name, '''') WHERE icon IS NULL OR icon = ''''',
    'SELECT 1');
PREPARE icon_release_statement FROM @sql;
EXECUTE icon_release_statement;
DEALLOCATE PREPARE icon_release_statement;
-- icon_file_id 无法映射为 Lucide 标识，不生成虚构图标。
