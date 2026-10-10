-- 在声明式同步之前保留来源数据，避免开发环境同步时先删除来源列。
-- 新库无需预处理；生产环境的来源列由声明式删除保护保留。
-- MariaDB DDL 隐式提交，因此在写入成功记录前中断时，文件必须能够重新执行。
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
