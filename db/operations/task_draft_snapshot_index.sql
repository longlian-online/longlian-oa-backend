-- 仅供已部署旧草稿唯一索引的环境，在审核后随本次升级执行。
-- prod 同步默认保护索引删除；应用新版本前必须移除“一任务一行”的旧约束。
ALTER TABLE task_draft DROP INDEX IF EXISTS uk_task_draft_instance;
