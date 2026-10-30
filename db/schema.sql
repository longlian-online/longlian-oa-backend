-- Create "admin" table
CREATE TABLE `admin` (
  `id` bigint NOT NULL,
  `username` varchar(64) NOT NULL COMMENT "用户名",
  `password` varchar(128) NOT NULL COMMENT "密码",
  `role` varchar(32) NOT NULL COMMENT "角色：root/normal",
  `last_login_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "上一次登录时间",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_username` (`username`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "管理员表";
-- Create "base_task" table
CREATE TABLE `base_task` (
  `id` bigint NOT NULL COMMENT "原子任务ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(100) NOT NULL COMMENT "任务名称（标题）（如：创建/翻译/校对）",
  `icon` varchar(100) NULL COMMENT "Lucide 图标组件名",
  `description` varchar(500) NULL DEFAULT "" COMMENT "任务说明（简介）",
  `meta_schema` json NULL COMMENT "元数据字段定义(JSON数组)",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "原子任务表（最小任务单元）";
-- Create "data_migration" table
CREATE TABLE `data_migration` (
  `filename` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT "迁移 SQL 文件名，成功后不可改名或修改",
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "成功执行时间",
  PRIMARY KEY (`filename`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "已成功执行的数据迁移记录";
-- Create "email_verify_otp" table
CREATE TABLE `email_verify_otp` (
  `id` bigint NOT NULL COMMENT "邮箱验证码ID",
  `otp_id` bigint NOT NULL COMMENT "关联验证码ID",
  `receiver` varchar(255) NOT NULL COMMENT "接收者邮箱",
  `business_type` tinyint NOT NULL DEFAULT 0 COMMENT "业务类型 0-登录 1-注册 2-忘记密码 3-绑定邮箱",
  `send_status` tinyint NOT NULL DEFAULT 0 COMMENT "发送状态 0-待发送 1-发送成功 2-发送失败",
  `sent_at` datetime NULL COMMENT "发送成功时间",
  `failed_at` datetime NULL COMMENT "发送失败时间",
  `fail_reason` varchar(500) NOT NULL DEFAULT "" COMMENT "发送失败原因",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_receiver_send_status_created_at` (`receiver`, `send_status`, `created_at`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "邮箱验证码扩展表";
-- Create "group_application" table
CREATE TABLE `group_application` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "目标组织ID",
  `otp_id` bigint NULL COMMENT "提交申请时使用的邀请码记录ID",
  `user_id` bigint NULL COMMENT "申请人ID（注册待审时为空）",
  `password_hash` varchar(100) NULL COMMENT "注册待审密码哈希，审批后清空",
  `status` tinyint NOT NULL DEFAULT 0 COMMENT "状态：0-待审核 1-通过 2-拒绝",
  `reviewer_id` bigint NULL COMMENT "审核人ID",
  `reviewed_at` datetime NULL COMMENT "审核时间",
  `review_remark` varchar(500) NULL DEFAULT "" COMMENT "审核备注",
  `application_type` tinyint NULL COMMENT "申请入组的类型：0-注册入组 1-已注册用户入组",
  `username` varchar(50) NULL COMMENT "用户名",
  `nickname` varchar(50) NULL DEFAULT "" COMMENT "昵称",
  `email` varchar(100) NULL DEFAULT "" COMMENT "邮箱",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_group_application_otp_id` (`otp_id`),
  INDEX `idx_group_application_org_status_email` (`org_id`, `status`, `email`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "入组申请表";
-- Create "item" table
CREATE TABLE `item` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `title` varchar(100) NOT NULL COMMENT "项目名称",
  `sort` bigint NOT NULL DEFAULT 0 COMMENT "企划内项目展示顺序",
  `task_template_id` bigint NOT NULL COMMENT "创建时关联的任务模板ID（仅记录溯源）",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_item_project_order` (`project_id`, `deleted_at`, `sort`, `id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "项目表";
-- Create "item_task_flow" table
CREATE TABLE `item_task_flow` (
  `id` bigint NOT NULL COMMENT "项目任务流ID",
  `item_id` bigint NOT NULL COMMENT "所属项目ID",
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `task_template_id` bigint NOT NULL COMMENT "关联任务模板ID",
  `name` varchar(100) NOT NULL COMMENT "任务流名称（继承自任务模板）",
  `description` varchar(500) NULL DEFAULT "" COMMENT "任务流说明",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_item_id` (`item_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "项目任务流表";
-- Create "item_task_node" table
CREATE TABLE `item_task_node` (
  `id` bigint NOT NULL COMMENT "项目任务节点ID",
  `item_task_flow_id` bigint NOT NULL COMMENT "所属项目任务流ID",
  `item_id` bigint NOT NULL COMMENT "所属项目ID",
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `base_task_id` bigint NOT NULL COMMENT "关联原子任务ID",
  `name` varchar(100) NOT NULL COMMENT "任务名称",
  `meta_schema` json NULL COMMENT "节点元数据字段定义快照(JSON数组)",
  `sort` int NOT NULL DEFAULT 0 COMMENT "步骤顺序",
  `parallel_sort` int NULL COMMENT "并行组号排序",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_flow_sort` (`item_task_flow_id`, `sort`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "项目任务节点表";
-- Create "one_time_password" table
CREATE TABLE `one_time_password` (
  `id` bigint unsigned NOT NULL,
  `code` varchar(128) NOT NULL COMMENT "验证码",
  `expired_at` datetime NULL COMMENT "过期时间，为空表示不过期",
  `used_at` datetime NULL COMMENT "使用时间",
  `max_uses` int NULL COMMENT "使用上限，为空表示不限次数",
  `use_count` int NOT NULL DEFAULT 0 COMMENT "已使用次数",
  `biz_type` tinyint NOT NULL COMMENT "业务类型 1-邀请创建组织 2-邀请加入组织 3-邮箱验证码",
  `status` tinyint NOT NULL COMMENT "状态 0-待使用 1-已使用",
  `creator_id` bigint NOT NULL COMMENT "创建者 ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_code_expired_at` (`code`, `expired_at`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "一次性密码（otp）表";
-- Create "organization" table
CREATE TABLE `organization` (
  `id` bigint NOT NULL COMMENT "组织ID",
  `name` varchar(100) NOT NULL COMMENT "组织名称",
  `avatar_file_id` bigint NULL COMMENT "组织头像",
  `description` varchar(500) NULL DEFAULT "" COMMENT "组织简介",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "组织表";
-- Create "organization_create_otp" table
CREATE TABLE `organization_create_otp` (
  `id` bigint NOT NULL COMMENT "邀请ID",
  `otp_id` bigint NOT NULL COMMENT "关联验证码ID",
  `invited_user_id` bigint NULL COMMENT "被邀请用户ID",
  `org_id` bigint NULL COMMENT "创建成功后的组织ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "邀请创建组织表";
-- Create "organization_join_otp" table
CREATE TABLE `organization_join_otp` (
  `id` bigint NOT NULL COMMENT "邀请ID",
  `otp_id` bigint NOT NULL COMMENT "关联验证码ID",
  `org_id` bigint NOT NULL COMMENT "目标组织ID",
  `invited_user_id` bigint NULL COMMENT "被邀请用户ID",
  `org_member_id` bigint NULL COMMENT "加入成功后的组织成员ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "邀请加入组织表";
-- Create "organization_member" table
CREATE TABLE `organization_member` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "组织ID",
  `user_id` bigint NOT NULL COMMENT "用户ID",
  `org_role` varchar(20) NOT NULL DEFAULT "ORG_USER" COMMENT "组织内角色：ORG_OWNER/ORG_ADMIN/ORG_USER",
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "入组时间",
  `last_submitted_at` datetime NULL COMMENT "上次提交任务时间",
  `submit_count` int NOT NULL DEFAULT 0 COMMENT "任务提交总数",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_org_member_user` (`org_id`, `user_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "组织成员表";
-- Create "organization_preset_tag" table
CREATE TABLE `organization_preset_tag` (
  `id` bigint NOT NULL COMMENT "组织预制标签ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(50) NOT NULL COMMENT "标签名",
  `color` varchar(16) NULL COMMENT "颜色，空为默认。blue/green/amber/rose/violet",
  `priority` int NOT NULL DEFAULT 0 COMMENT "优先级，仅用户端下拉按值降序",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_org_preset_tag_org` (`org_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "组织预制标签表";
-- Create "organization_preset_field" table
CREATE TABLE `organization_preset_field` (
  `id` bigint NOT NULL COMMENT "组织预置表单字段ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(50) NOT NULL COMMENT "字段名",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_org_preset_field_org` (`org_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "组织预置表单字段表";
-- Create "outbox_message" table
CREATE TABLE `outbox_message` (
  `id` bigint NOT NULL COMMENT "消息ID",
  `type` tinyint NOT NULL COMMENT "投递类型 1-INBOX_EMAIL",
  `dedupe_key` varchar(64) NOT NULL COMMENT "同一类型内的幂等键",
  `payload` json NOT NULL COMMENT "该 type 专用的 JSON",
  `status` tinyint NOT NULL COMMENT "1-PENDING 2-PROCESSING 3-SENT 4-FAILED 5-SKIPPED",
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT "已领取次数，含崩溃后再次领取",
  `available_at` datetime NOT NULL COMMENT "PENDING 可领取时间",
  `locked_until` datetime NULL COMMENT "PROCESSING 租约",
  `last_error` varchar(500) NULL COMMENT "最近一次失败或跳过原因，不含邮箱地址和正文",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_outbox_message_dedupe` (`type`, `dedupe_key`),
  INDEX `idx_outbox_message_pending` (`status`, `available_at`, `id`),
  INDEX `idx_outbox_message_processing` (`status`, `locked_until`, `id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "通用 outbox。一种 type 对应一个投递器";
-- Create "permission" table
CREATE TABLE `permission` (
  `id` bigint NOT NULL COMMENT "权限ID",
  `perm_code` varchar(100) NOT NULL COMMENT "权限编码（如：org:member:disable）",
  `perm_name` varchar(50) NOT NULL COMMENT "权限名称",
  `perm_type` tinyint NOT NULL COMMENT "类型 1-菜单 2-按钮 3-接口",
  `path` varchar(200) NULL DEFAULT "" COMMENT "前端路由/接口路径",
  `parent_id` bigint NULL DEFAULT 0 COMMENT "父权限ID（用于菜单层级）",
  `sort` int NULL DEFAULT 0 COMMENT "排序（前端展示顺序）",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间",
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间",
  `deleted_at` datetime NULL COMMENT "软删除时间",
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_perm_code` (`perm_code`) COMMENT "权限编码唯一"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "权限表（冻结：当前授权使用 organization_member.org_role）";
-- Create "project" table
CREATE TABLE `project` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `type_id` bigint NOT NULL COMMENT "企划类型ID",
  `title` varchar(100) NOT NULL COMMENT "企划名称",
  `alias` varchar(100) NULL DEFAULT "" COMMENT "别名",
  `metadata` json NULL COMMENT "扩展信息(JSON字符串)",
  `cover_file_id` bigint NULL COMMENT "封面图",
  `thumbnail_file_id` bigint NULL COMMENT "缩略图",
  `description` text NULL COMMENT "简介",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-进行中 2-已完成 3-已归档",
  `resource_status` tinyint NOT NULL DEFAULT 1 COMMENT "资源状态 1-启用 0-禁用",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_org_type` (`org_id`, `type_id`),
  FULLTEXT INDEX `ft_project_title` (`title`),
  FULLTEXT INDEX `ft_project_alias` (`alias`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "企划表";
-- Create "project_type" table
CREATE TABLE `project_type` (
  `id` bigint NOT NULL COMMENT "企划类型ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(50) NOT NULL COMMENT "类型名称（如：漫画/小说/美术/视频）",
  `priority` int NOT NULL DEFAULT 0 COMMENT "优先级，仅用户端类型列表按值降序",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "企划类型表";
-- Create "project_type_custom_field" table
CREATE TABLE `project_type_custom_field` (
  `id` bigint NOT NULL COMMENT "企划类型默认自定义字段ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `type_id` bigint NOT NULL COMMENT "企划类型ID",
  `preset_field_id` bigint NULL COMMENT "预置表单字段ID；自行编辑的字段为空",
  `name` varchar(50) NULL COMMENT "字段名；引用预置字段时为空，展示时取预置字段当前名称",
  `required` tinyint NOT NULL DEFAULT 0 COMMENT "是否必填 1-必填 0-选填",
  `sort` int NOT NULL DEFAULT 0 COMMENT "表单内顺序，从 0 开始",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_type_custom_field_type` (`type_id`, `sort`),
  INDEX `idx_type_custom_field_preset` (`preset_field_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "企划类型默认自定义表单。不建唯一索引，避免软删除后无法再选同一预置字段";
-- Create "project_workshop" table
CREATE TABLE `project_workshop` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "企划ID",
  `user_id` bigint NOT NULL COMMENT "添加人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  `active_guard` bigint GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, `id`)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_project_workshop_active` (`project_id`, `user_id`, `active_guard`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "企划-工坊关联表（用户添加企划）";
-- Create "resource" table
CREATE TABLE `resource` (
  `id` bigint NOT NULL COMMENT "文件ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `storage_type` tinyint NOT NULL COMMENT "存储类型 1-本地存储 2-阿里云对象存储 3-腾讯云对象存储",
  `storage_key` varchar(255) NOT NULL COMMENT "存储唯一标识（如OSS的objectKey/本地文件路径）",
  `file_name` varchar(255) NOT NULL COMMENT "原始文件名",
  `file_ext` varchar(20) NOT NULL COMMENT "文件扩展名",
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT "文件大小",
  `file_mime` varchar(100) NULL DEFAULT "" COMMENT "文件MIME类型",
  `biz_type` varchar(50) NOT NULL COMMENT "业务类型（如：avatar/cover/task_submit）",
  `biz_id` bigint NOT NULL COMMENT "业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）",
  `process_status` tinyint NOT NULL DEFAULT 0 COMMENT "状态 0-待上传 1-已激活 2-已废弃 3-已上传待绑定",
  `deprecated_at` datetime NULL COMMENT "进入保留期的时间；从未绑定的清理不写",
  `storage_cleaned_at` datetime NULL COMMENT "存储实际文件清理完成时间",
  `cleanup_attempts` int NOT NULL DEFAULT 0 COMMENT "存储清理尝试次数",
  `cleanup_next_at` datetime NULL COMMENT "存储清理下次重试时间",
  `cleanup_error` varchar(1000) NULL COMMENT "最近一次存储清理失败信息",
  `size_checked_at` datetime NULL COMMENT "最近一次与对象实际大小核对的时间",
  `creator_id` bigint NOT NULL COMMENT "上传人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_storage_key` (`storage_key`),
  INDEX `idx_resource_cleanup` (`process_status`, `storage_cleaned_at`, `cleanup_next_at`),
  INDEX `idx_resource_retention` (`process_status`, `storage_cleaned_at`, `deprecated_at`),
  INDEX `idx_resource_org_usage` (`org_id`, `biz_type`, `process_status`, `storage_cleaned_at`),
  INDEX `idx_resource_size_check` (`storage_cleaned_at`, `size_checked_at`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "通用文件存储表";
-- Create "role" table
CREATE TABLE `role` (
  `id` bigint NOT NULL COMMENT "角色ID",
  `role_code` varchar(50) NOT NULL COMMENT "角色编码（如：ORG_ADMIN）",
  `role_name` varchar(50) NOT NULL COMMENT "角色名称",
  `description` varchar(200) NULL DEFAULT "" COMMENT "角色描述",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_role_code` (`role_code`) COMMENT "角色编码唯一"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "角色表（冻结：当前授权使用 organization_member.org_role）";
-- Create "role_permission" table
CREATE TABLE `role_permission` (
  `id` bigint NOT NULL COMMENT "主键ID",
  `role_id` bigint NOT NULL COMMENT "角色ID",
  `permission_id` bigint NOT NULL COMMENT "权限ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_role_perm` (`role_id`, `permission_id`) COMMENT "角色-权限唯一"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "角色权限关联表（冻结：当前授权使用 organization_member.org_role）";
-- Create "scheduled_task_log" table
CREATE TABLE `scheduled_task_log` (
  `id` bigint NOT NULL COMMENT "日志ID",
  `task_name` varchar(100) NOT NULL COMMENT "任务名称",
  `trigger_source` tinyint NOT NULL COMMENT "触发来源 1-SCHEDULED(定时触发) 2-MANUAL(手动触发)",
  `execute_time_param` datetime NOT NULL COMMENT "上层传递的执行时间",
  `status` tinyint NOT NULL COMMENT "执行状态 1-RUNNING(执行中) 2-SUCCESS(成功) 3-FAILED(失败)",
  `error_message` text NULL COMMENT "失败时的错误信息",
  `execution_id` varchar(64) NOT NULL COMMENT "执行追踪ID",
  `triggered_by` bigint NULL COMMENT "手动触发人ID（定时触发时为NULL）",
  `started_at` datetime NOT NULL COMMENT "开始执行时间",
  `ended_at` datetime NULL COMMENT "结束执行时间",
  `duration_ms` bigint NULL COMMENT "执行耗时（毫秒）",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间",
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间",
  `deleted_at` datetime NULL COMMENT "软删除时间",
  PRIMARY KEY (`id`),
  INDEX `idx_started_at` (`started_at`) COMMENT "按执行时间查询",
  INDEX `idx_task_name` (`task_name`) COMMENT "按任务名查询"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "定时任务执行日志表";
-- Create "task_instance" table
CREATE TABLE `task_instance` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `item_id` bigint NOT NULL COMMENT "所属项目ID",
  `item_task_node_id` bigint NOT NULL COMMENT "关联项目任务节点ID",
  `task_flow_id` bigint NOT NULL COMMENT "关联任务流ID",
  `assignee_id` bigint NULL COMMENT "接取人ID",
  `claimed_at` datetime NULL COMMENT "接取时间（接取写入，放弃清空，打回/重置保留）",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "任务状态 1-PENDING(待接取) 2-CLAIMED(待提交) 3-COMPLETED(已完成)",
  `revision` bigint NOT NULL DEFAULT 0 COMMENT "任务状态修订号（每次状态写入递增，防止旧操作重放）",
  `submitted_at` datetime NULL COMMENT "提交时间",
  `completed_at` datetime NULL COMMENT "完成时间",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_item_id` (`item_id`) COMMENT "按项目查询任务实例"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "任务实例表";
-- Create "task_submission" table
CREATE TABLE `task_submission` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `item_id` bigint NOT NULL COMMENT "所属项目ID",
  `task_instance_id` bigint NOT NULL COMMENT "所属任务实例ID",
  `item_task_node_id` bigint NOT NULL COMMENT "关联项目任务节点ID",
  `submitter_id` bigint NOT NULL COMMENT "提交人ID",
  `version_no` int NULL COMMENT "提交版本号（任务实例内从 1 递增；存量数据由数据迁移回填）",
  `metadata` json NULL COMMENT "提交元数据(JSON对象，按节点 meta_schema 组织)",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)",
  `reviewer_id` bigint NULL COMMENT "审核人ID（打回操作人）",
  `reviewed_at` datetime NULL COMMENT "审核/打回时间",
  `review_comment` varchar(500) NULL DEFAULT "" COMMENT "审核/打回意见",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_submission_version` (`task_instance_id`, `version_no`),
  INDEX `idx_project_deleted_created` (`project_id`, `deleted_at`, `created_at`, `id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "任务提交记录表";
-- Create "task_template" table
CREATE TABLE `task_template` (
  `id` bigint NOT NULL COMMENT "任务模板ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(100) NOT NULL COMMENT "任务模板名称",
  `description` varchar(500) NULL DEFAULT "" COMMENT "模板说明",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `scope` tinyint NOT NULL DEFAULT 1 COMMENT "1-个人模板 2-组织通用模板",
  `ref_count` int NOT NULL DEFAULT 0 COMMENT "关联的项目数",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "任务模板表";
-- Create "task_template_node" table
CREATE TABLE `task_template_node` (
  `id` bigint NOT NULL COMMENT "任务模板节点ID",
  `task_template_id` bigint NOT NULL COMMENT "所属任务模板ID",
  `base_task_id` bigint NOT NULL COMMENT "关联原子任务ID",
  `sort` int NOT NULL DEFAULT 0 COMMENT "步骤顺序",
  `parallel_sort` int NULL COMMENT "并行组号排序",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "任务模板节点表";
-- Create "token_blacklist" table
CREATE TABLE `token_blacklist` (
  `id` bigint NOT NULL COMMENT "主键ID",
  `token` varchar(512) NOT NULL COMMENT "Token字符串",
  `token_type` tinyint NOT NULL COMMENT "Token类型 1-用户端 2-管理端",
  `user_id` bigint NOT NULL COMMENT "用户/管理员ID",
  `reason` varchar(200) NULL DEFAULT "" COMMENT "加入黑名单原因",
  `expired_at` datetime NOT NULL COMMENT "Token过期时间",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间",
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间",
  PRIMARY KEY (`id`),
  INDEX `idx_expired` (`expired_at`) COMMENT "过期时间索引",
  INDEX `idx_user` (`token_type`, `user_id`) COMMENT "用户索引",
  UNIQUE INDEX `uk_token` (`token`) COMMENT "Token唯一索引"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "Token黑名单表";
-- Create "user" table
CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT "用户ID",
  `username` varchar(50) NOT NULL COMMENT "用户名（登录用）",
  `password` varchar(100) NOT NULL COMMENT "密码（加密存储）",
  `nickname` varchar(50) NOT NULL DEFAULT "" COMMENT "昵称（展示用）",
  `email` varchar(100) NULL DEFAULT NULL COMMENT "邮箱（登录/通知，未绑定时为NULL）",
  `avatar_file_id` bigint NULL COMMENT "用户头像",
  `default_org_id` bigint NOT NULL DEFAULT 0 COMMENT "默认组织ID",
  `config` json NOT NULL DEFAULT '{}' COMMENT "用户配置",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_email` (`email`) COMMENT "邮箱唯一",
  UNIQUE INDEX `uk_username` (`username`) COMMENT "用户名唯一"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "系统用户表";
-- Create "user_operation_log" table
CREATE TABLE `user_operation_log` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT "操作人ID",
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `item_id` bigint NULL COMMENT "所属项目ID",
  `operation_type` tinyint NOT NULL COMMENT "操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务) 7-TASK_ASSIGN(分配任务)",
  `request_body` json NULL COMMENT "操作请求体",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "操作时间",
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "用户操作记录表";
-- Create "user_role" table
CREATE TABLE `user_role` (
  `id` bigint NOT NULL COMMENT "主键ID",
  `user_id` bigint NOT NULL COMMENT "用户ID",
  `role_id` bigint NOT NULL COMMENT "角色ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_role` (`user_id`, `role_id`) COMMENT "用户-角色唯一"
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "用户角色关联表（冻结：当前授权使用 organization_member.org_role）";
-- Create "deletion_request" table
CREATE TABLE `deletion_request` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `target_type` tinyint NOT NULL COMMENT "目标类型 1-PROJECT(企划) 2-ITEM(项目)",
  `target_id` bigint NOT NULL COMMENT "目标ID",
  `project_id` bigint NOT NULL COMMENT "所属企划ID；目标为企划时与 target_id 相同",
  `target_title` varchar(100) NOT NULL COMMENT "提交时的名称",
  `reason` varchar(500) NOT NULL COMMENT "删除原因",
  `requester_id` bigint NOT NULL COMMENT "申请人ID",
  `status` tinyint NOT NULL DEFAULT 0 COMMENT "状态 0-PENDING(待审核) 1-APPROVED(通过) 2-REJECTED(拒绝)",
  `reviewer_id` bigint NULL COMMENT "审核人ID",
  `reviewed_at` datetime NULL COMMENT "审核时间",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  `pending_guard` bigint GENERATED ALWAYS AS (IF(`status` = 0 AND `deleted_at` IS NULL, 0, `id`)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_deletion_request_pending` (`target_type`, `target_id`, `pending_guard`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "删除申请表";
-- Create "user_notification" table
CREATE TABLE `user_notification` (
  `id` bigint NOT NULL COMMENT "通知ID",
  `org_id` bigint NULL COMMENT "组织作用域",
  `recipient_user_id` bigint NOT NULL COMMENT "固定收件人ID",
  `kind` tinyint NOT NULL COMMENT "类型 1-TASK_READY 2-ITEM_COMPLETED 3-TASK_ASSIGNED 4-JOIN_APPLICATION 5-DELETION_REQUEST",
  `source_id` bigint NOT NULL COMMENT "类型定义的来源ID",
  `trigger_id` bigint NOT NULL COMMENT "类型定义的一次触发ID",
  `read_at` datetime NULL COMMENT "已读时间，NULL为未读",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_notification_event` (`recipient_user_id`, `kind`, `source_id`, `trigger_id`),
  INDEX `idx_user_notification_inbox` (`org_id`, `recipient_user_id`, `created_at`, `id`),
  INDEX `idx_user_notification_unread` (`org_id`, `recipient_user_id`, `read_at`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "站内通知表";
-- Create "product_release" table
CREATE TABLE `product_release` (
  `id` bigint NOT NULL COMMENT "产品版本ID",
  `version` varchar(32) NOT NULL COMMENT "产品版本，x.y.z，不含v",
  `frontend_version` varchar(32) NOT NULL COMMENT "绑定的前端版本，x.y.z，不含v",
  `backend_version` varchar(32) NOT NULL COMMENT "绑定的后端版本，x.y.z，不含v",
  `title` varchar(40) NOT NULL COMMENT "标题，草稿可为空字符串",
  `summary` varchar(120) NOT NULL COMMENT "摘要，草稿可为空字符串",
  `status` tinyint NOT NULL DEFAULT 0 COMMENT "状态 0-DRAFT(草稿) 1-PUBLISHED(已发布) 2-WITHDRAWN(已撤回)",
  `published_at` datetime(6) NULL COMMENT "发布时间，发布后不变；微秒精度用于区分同一秒内的连续发布",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `published_pair_guard` bigint GENERATED ALWAYS AS (IF(`status` = 1, 0, `id`)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_product_release_version` (`version`),
  UNIQUE INDEX `uk_product_release_published_pair` (`frontend_version`, `backend_version`, `published_pair_guard`),
  INDEX `idx_product_release_published` (`status`, `published_at`, `id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "产品版本";
-- Create "product_release_change" table
CREATE TABLE `product_release_change` (
  `id` bigint NOT NULL COMMENT "变更条目ID",
  `release_id` bigint NOT NULL COMMENT "产品版本ID",
  `kind` tinyint NOT NULL COMMENT "分组 1-ADDED(新增) 2-CHANGED(调整) 3-FIXED(修复)",
  `sort` int NOT NULL COMMENT "组内顺序，从0开始",
  `content` varchar(80) NOT NULL COMMENT "条目正文",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_product_release_change` (`release_id`, `kind`, `sort`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "产品版本变更条目";
-- Create "user_product_release_state" table
CREATE TABLE `user_product_release_state` (
  `user_id` bigint NOT NULL COMMENT "用户ID",
  `last_seen_published_at` datetime(6) NOT NULL COMMENT "已读到的发布时间，只向前移动；与发布时间同为微秒精度",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci COMMENT "用户产品版本已读位置";
