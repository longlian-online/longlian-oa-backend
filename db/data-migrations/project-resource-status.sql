-- 旧版本会将项目业务状态直接覆盖为 0（禁用），原业务进度已无法从当前记录还原。
-- 执行前请停止旧版本应用并结合备份或审计记录核对；无法还原时统一回退为“进行中”。
UPDATE project
SET resource_status = 0,
    status = 1
WHERE status = 0;
