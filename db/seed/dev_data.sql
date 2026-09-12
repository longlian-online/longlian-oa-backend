-- 开发环境初始化数据（仅用于本地开发，不纳入版本化迁移）

INSERT IGNORE INTO `admin` (`id`, `username`, `password`, `role`, `last_login_at`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'root', '$2y$10$x1expS7FECBmkKNkA6ZEeOAWFXn3zFXkcPoHF6JL.xe.wbCEzI2l2', 'root', '2026-06-09 00:00:00', '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `email`, `avatar_file_id`, `default_org_id`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'user', '$2y$10$x1expS7FECBmkKNkA6ZEeOAWFXn3zFXkcPoHF6JL.xe.wbCEzI2l2', 'user', '123456@qq.com', NULL, '0', 1, '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);
