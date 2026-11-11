-- 项目状态重新作为已完成的依据。存量行一律回到进行中，包括旧列里残留的已完成和已公布。
-- 那些值来自已下线的公布功能，不能当成现在的已完成。
UPDATE item
SET status = 1, completed_at = NULL, updated_at = updated_at
WHERE status <> 1 OR completed_at IS NOT NULL;
