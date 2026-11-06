-- 将先前直接绑定任务的未提交附件归属到草稿，文件 ID 和存储路径不变。
-- 当前有效草稿优先；仅旧草稿引用的附件留到任务放弃或删除时统一回收。
-- 正式成果引用的资源保持原归属，不修改历史附件。
UPDATE resource r
JOIN (
  SELECT d.org_id, d.task_instance_id, files.file_id,
         COALESCE(MAX(CASE WHEN d.status = 1 THEN d.id END), MAX(d.id)) AS draft_id
  FROM task_draft d
  JOIN JSON_TABLE(
    COALESCE(JSON_EXTRACT(d.metadata, '$.*.fileId'), '[]'),
    '$[*]' COLUMNS (file_id BIGINT PATH '$')
  ) files ON 1 = 1
  WHERE d.status IN (1, 4)
  GROUP BY d.org_id, d.task_instance_id, files.file_id
) drafts ON drafts.file_id = r.id
        AND drafts.org_id = r.org_id
        AND drafts.task_instance_id = r.biz_id
LEFT JOIN (
  SELECT DISTINCT files.file_id
  FROM task_submission s
  JOIN JSON_TABLE(
    COALESCE(JSON_EXTRACT(s.metadata, '$.*.fileId'), '[]'),
    '$[*]' COLUMNS (file_id BIGINT PATH '$')
  ) files ON 1 = 1
) submitted ON submitted.file_id = r.id
SET r.biz_type = 'task_draft', r.biz_id = drafts.draft_id
WHERE r.biz_type = 'task_submit'
  AND submitted.file_id IS NULL;
