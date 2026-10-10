-- Atlas 已创建目标列并保留旧列；新库没有旧列时不需要复制数据。
SET @sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_task' AND COLUMN_NAME = 'icon_name'
), 'UPDATE base_task SET icon = NULLIF(icon_name, '''') WHERE icon IS NULL OR icon = ''''', 'SELECT 1');
PREPARE icon_migration FROM @sql;
EXECUTE icon_migration;
DEALLOCATE PREPARE icon_migration;
