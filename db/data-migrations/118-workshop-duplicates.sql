-- 在添加项目工坊唯一约束前，保留每组重复记录中 ID 最大的一条有效记录。
UPDATE project_workshop old_row
JOIN project_workshop keep_row
  ON keep_row.project_id = old_row.project_id
 AND keep_row.user_id = old_row.user_id
 AND keep_row.deleted_at IS NULL
 AND old_row.deleted_at IS NULL
 AND keep_row.id > old_row.id
SET old_row.deleted_at = CURRENT_TIMESTAMP;
