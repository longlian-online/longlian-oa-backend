ALTER TABLE `resource`
    ADD COLUMN `storage_cleaned_at` datetime DEFAULT NULL COMMENT '物理存储清理完成时间',
    ADD COLUMN `cleanup_attempts` int NOT NULL DEFAULT 0 COMMENT '物理清理尝试次数',
    ADD COLUMN `cleanup_next_at` datetime DEFAULT NULL COMMENT '下次允许物理清理时间',
    ADD COLUMN `cleanup_error` varchar(1000) DEFAULT NULL COMMENT '最近一次物理清理失败信息',
    ADD INDEX `idx_resource_cleanup` (`process_status`, `storage_cleaned_at`, `cleanup_next_at`);
