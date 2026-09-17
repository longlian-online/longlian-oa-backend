-- 部署基础数据。当前仅初始化管理端 root 账号。
-- DEFAULT_ADMIN_PASSWORD_HASH 由部署环境提供；后续基础配置统一追加到此文件。
INSERT IGNORE INTO `admin` (`id`, `username`, `password`, `role`, `last_login_at`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'root', '__DEFAULT_ADMIN_PASSWORD_HASH__', 'root', '2026-06-09 00:00:00', '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);
