-- 草稿附件直接归属任务；保留文件 ID、对象路径和原有存储状态。
UPDATE resource r
JOIN task_draft d ON d.id = r.biz_id AND d.org_id = r.org_id
SET r.biz_type = 'task_submit', r.biz_id = d.task_instance_id
WHERE r.biz_type = 'task_draft' AND r.biz_id <> 0;

-- 未完成的旧封存候选继续由通用未绑定上传清理流程回收。
UPDATE resource
SET biz_type = 'task_submit'
WHERE biz_type = 'task_draft' AND biz_id = 0;
