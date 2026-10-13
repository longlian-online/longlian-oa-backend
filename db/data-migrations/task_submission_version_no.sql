-- 版本化提交记录：为存量提交回填版本号。
-- 仅处理 version_no IS NULL 的存量行（未被应用分配过版本），按实例内创建顺序从 1 递增。
-- 幂等：回填完成后不再存在 NULL 行，重跑无效果。
UPDATE task_submission ts
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY task_instance_id ORDER BY created_at, id) AS rn
  FROM task_submission
) t ON t.id = ts.id
SET ts.version_no = t.rn
WHERE ts.version_no IS NULL;
