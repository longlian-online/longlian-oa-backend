-- Atlas 只负责结构同步；此文件维护新结构上线后必须成立的数据兼容约束。
-- SQL 必须幂等，因为每次 db/migrate.sh <dev|prod> apply 都会执行。

-- 旧版本用 project.status = 0 表示禁用。新模型把可用性迁到 resource_status，
-- 而业务进度枚举只接受 1/2/3；旧进度无法还原时统一回退为“进行中”。
UPDATE `project`
SET `resource_status` = 0,
    `status` = 1
WHERE `status` = 0;
