-- 开发环境初始化数据（仅用于本地开发，不纳入版本化迁移）
-- 本地账号：
--   管理端：root / 123456（无需组织）
--   用户端：user / 123456 / 123456@qq.com，默认组织 id=1，角色 ORG_ADMIN

INSERT IGNORE INTO `admin` (`id`, `username`, `password`, `role`, `last_login_at`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'root', '$2y$10$x1expS7FECBmkKNkA6ZEeOAWFXn3zFXkcPoHF6JL.xe.wbCEzI2l2', 'root', '2026-06-09 00:00:00', '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);

INSERT IGNORE INTO `organization` (`id`, `name`, `creator_id`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', '开发组织', '1', 1, '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `email`, `avatar_file_id`, `default_org_id`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'user', '$2y$10$x1expS7FECBmkKNkA6ZEeOAWFXn3zFXkcPoHF6JL.xe.wbCEzI2l2', 'user', '123456@qq.com', NULL, '1', 1, '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);

INSERT IGNORE INTO `organization_member` (`id`, `org_id`, `user_id`, `org_role`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', '1', '1', 'ORG_ADMIN', 1, '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);

UPDATE `user` SET `default_org_id` = 1 WHERE `id` = 1 AND (`default_org_id` IS NULL OR `default_org_id` = 0);
