-- 我的待办（#223）：为存量有接取人的任务实例回填 claimed_at。
-- 优先取操作日志中该实例、该接取人最近一次 TASK_CLAIM(operation_type=1) 的时间；
-- 日志缺失时回退为 updated_at。
-- 幂等：仅处理 assignee_id IS NOT NULL AND claimed_at IS NULL 的行，重跑无效果。
UPDATE task_instance ti
LEFT JOIN (
  SELECT CAST(JSON_VALUE(l.request_body, '$.instanceId') AS UNSIGNED) AS instance_id,
         l.user_id AS user_id,
         MAX(l.created_at) AS claimed_at
  FROM user_operation_log l
  WHERE l.operation_type = 1
    AND l.deleted_at IS NULL
    AND l.request_body IS NOT NULL
  GROUP BY CAST(JSON_VALUE(l.request_body, '$.instanceId') AS UNSIGNED), l.user_id
) c ON c.instance_id = ti.id AND c.user_id = ti.assignee_id
SET ti.claimed_at = COALESCE(c.claimed_at, ti.updated_at)
WHERE ti.assignee_id IS NOT NULL
  AND ti.claimed_at IS NULL;
