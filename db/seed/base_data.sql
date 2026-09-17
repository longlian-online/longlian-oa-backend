-- 部署基础数据。当前仅初始化管理端账号：root / 123456。
-- 后续部署所需的基础配置统一追加到此文件；已有记录不会被覆盖。
INSERT IGNORE INTO `admin` (`id`, `username`, `password`, `role`, `last_login_at`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'root', '$2y$10$x1expS7FECBmkKNkA6ZEeOAWFXn3zFXkcPoHF6JL.xe.wbCEzI2l2', 'root', '2026-06-09 00:00:00', '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);
