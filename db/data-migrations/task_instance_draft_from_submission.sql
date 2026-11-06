-- 把尚未合并的暂存提交挪到任务实例上，并把暂存附件改挂到任务实例。
-- 幂等：暂存行删除后不再匹配，重跑无效果。
UPDATE task_instance ti
JOIN task_submission ts ON ts.task_instance_id = ti.id AND ts.status = 4 AND ts.deleted_at IS NULL
SET ti.draft_metadata = ts.metadata,
    ti.draft_updated_at = ts.updated_at
WHERE ti.draft_metadata IS NULL;

UPDATE resource r
JOIN task_submission ts ON ts.status = 4 AND r.biz_type = 'task_submit' AND r.biz_id = ts.id
SET r.biz_id = ts.task_instance_id,
    r.updated_at = CURRENT_TIMESTAMP
WHERE r.process_status = 1 AND r.storage_cleaned_at IS NULL;

DELETE FROM task_submission WHERE status = 4;
