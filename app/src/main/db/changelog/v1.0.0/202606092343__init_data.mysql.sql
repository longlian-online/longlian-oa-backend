-- liquibase formatted sql

-- changeset asukai:default-admin splitStatements:false context:dev
INSERT INTO `admin` (`id`, `username`, `password`, `role`, `last_login_at`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'root', '$2a$10$s2suXXpI/zDLdB1Tctb/yuLVUe4LJeMTmGA.swToRG/vVy5aDqPSG', 'root', '2026-06-09 00:00:00', '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);

-- changeset asukai:default-user splitStatements:false context:dev
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `email`, `avatar_file_id`, `default_org_id`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES ('1', 'user', '$2a$10$s2suXXpI/zDLdB1Tctb/yuLVUe4LJeMTmGA.swToRG/vVy5aDqPSG', 'user', '123456@qq.com', NULL, '0', 1, '2026-06-10 00:00:00', '2026-06-10 00:00:00', NULL);