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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "管理员表";
-- Create "base_task" table
CREATE TABLE `base_task` (
  `id` bigint NOT NULL COMMENT "原子任务ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(100) NOT NULL COMMENT "任务名称（标题）（如：创建/翻译/校对）",
  `icon_file_id` bigint NULL COMMENT "图标标识",
  `description` varchar(500) NULL DEFAULT "" COMMENT "任务说明（简介）",
  `meta_schema` json NULL COMMENT "元数据字段定义(JSON数组)",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "原子任务表（最小任务单元）";
-- Create "email_verify_otp" table
CREATE TABLE `email_verify_otp` (
  `id` bigint NOT NULL COMMENT "邮箱验证码ID",
  `otp_id` bigint NOT NULL COMMENT "关联验证码ID",
  `receiver` varchar(255) NOT NULL COMMENT "接收者邮箱",
  `business_type` tinyint NOT NULL DEFAULT 0 COMMENT "业务类型 0-登录 1-注册 2-忘记密码",
  `send_status` tinyint NOT NULL DEFAULT 0 COMMENT "发送状态 0-待发送 1-发送成功 2-发送失败",
  `sent_at` datetime NULL COMMENT "发送成功时间",
  `failed_at` datetime NULL COMMENT "发送失败时间",
  `fail_reason` varchar(500) NOT NULL DEFAULT "" COMMENT "发送失败原因",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_receiver_send_status_created_at` (`receiver`, `send_status`, `created_at`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "邮箱验证码扩展表";
-- Create "group_application" table
CREATE TABLE `group_application` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "目标组织ID",
  `user_id` bigint NOT NULL COMMENT "申请人ID",
  `status` tinyint NOT NULL DEFAULT 0 COMMENT "状态：0-待审核 1-通过 2-拒绝",
  `reviewer_id` bigint NULL COMMENT "审核人ID",
  `reviewed_at` datetime NULL COMMENT "审核时间",
  `review_remark` varchar(500) NULL DEFAULT "" COMMENT "审核备注",
  `application_type` tinyint NULL COMMENT "申请入组的类型：0-注册入组 1-已注册用户入组",
  `username` varchar(50) NULL COMMENT "用户名",
  `password` varchar(100) NULL COMMENT "密码",
  `nickname` varchar(50) NULL DEFAULT "" COMMENT "昵称",
  `email` varchar(100) NULL DEFAULT "" COMMENT "邮箱",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "入组申请表";
-- Create "item" table
CREATE TABLE `item` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `title` varchar(100) NOT NULL COMMENT "项目名称",
  `task_template_id` bigint NOT NULL COMMENT "创建时关联的任务模板ID（仅记录溯源）",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-进行中 2-已完成 3-已公布",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "项目表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "项目任务流表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "项目任务节点表";
-- Create "one_time_password" table
CREATE TABLE `one_time_password` (
  `id` bigint unsigned NOT NULL,
  `code` varchar(128) NOT NULL COMMENT "验证码",
  `expired_at` datetime NOT NULL COMMENT "过期时间",
  `used_at` datetime NULL COMMENT "使用时间",
  `biz_type` tinyint NOT NULL COMMENT "业务类型 1-邀请创建组织 2-邀请加入组织 3-邮箱验证码",
  `status` tinyint NOT NULL COMMENT "状态 0-待使用 1-已使用",
  `creator_id` bigint NOT NULL COMMENT "创建者 ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_code_expired_at` (`code`, `expired_at`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "一次性密码（otp）表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "组织表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "邀请创建组织表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "邀请加入组织表";
-- Create "organization_member" table
CREATE TABLE `organization_member` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "组织ID",
  `user_id` bigint NOT NULL COMMENT "用户ID",
  `org_role` varchar(20) NOT NULL DEFAULT "ORG_USER" COMMENT "组织内角色：ORG_ADMIN/ORG_USER",
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "入组时间",
  `last_submitted_at` datetime NULL COMMENT "上次提交任务时间",
  `submit_count` int NOT NULL DEFAULT 0 COMMENT "任务提交总数",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_org_member_user` (`org_id`, `user_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "组织成员表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "权限表";
-- Create "project" table
CREATE TABLE `project` (
  `id` bigint NOT NULL,
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `type_id` bigint NOT NULL COMMENT "企划类型ID",
  `title` varchar(100) NOT NULL COMMENT "企划名称",
  `alias` varchar(100) NULL DEFAULT "" COMMENT "别名",
  `metadata` json NULL COMMENT "扩展信息(JSON字符串)",
  `cover_file_id` bigint NULL COMMENT "封面图",
  `description` text NULL COMMENT "简介",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-进行中 2-已完成 3-已归档",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_org_type` (`org_id`, `type_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "企划表";
-- Create "project_type" table
CREATE TABLE `project_type` (
  `id` bigint NOT NULL COMMENT "企划类型ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `name` varchar(50) NOT NULL COMMENT "类型名称（如：漫画/小说/美术/视频）",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `creator_id` bigint NOT NULL COMMENT "创建人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "企划类型表";
-- Create "project_workshop" table
CREATE TABLE `project_workshop` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "企划ID",
  `user_id` bigint NOT NULL COMMENT "添加人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "企划-工坊关联表（用户添加企划）";
-- Create "resource" table
CREATE TABLE `resource` (
  `id` bigint NOT NULL COMMENT "文件ID",
  `org_id` bigint NOT NULL COMMENT "所属组织ID",
  `storage_type` tinyint NOT NULL COMMENT "存储类型 1-本地存储 2-云对象存储",
  `storage_key` varchar(255) NOT NULL COMMENT "存储唯一标识（如OSS的objectKey/本地文件路径）",
  `file_name` varchar(255) NOT NULL COMMENT "原始文件名",
  `file_ext` varchar(20) NOT NULL COMMENT "文件扩展名",
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT "文件大小",
  `file_mime` varchar(100) NULL DEFAULT "" COMMENT "文件MIME类型",
  `biz_type` varchar(50) NOT NULL COMMENT "业务类型（如：avatar/cover/task_submit）",
  `biz_id` bigint NOT NULL COMMENT "业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）",
  `process_status` tinyint NOT NULL DEFAULT 0 COMMENT "状态 0-未上传 1-已上传 3-已废弃",
  `creator_id` bigint NOT NULL COMMENT "上传人ID",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "通用文件存储表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "角色表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "角色权限关联表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "定时任务执行日志表";
-- Create "task_instance" table
CREATE TABLE `task_instance` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `item_id` bigint NOT NULL COMMENT "所属项目ID",
  `item_task_node_id` bigint NOT NULL COMMENT "关联项目任务节点ID",
  `task_flow_id` bigint NOT NULL COMMENT "关联任务流ID",
  `assignee_id` bigint NULL COMMENT "接取人ID",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "任务状态 1-PENDING(待接取) 2-CLAIMED(待提交) 3-COMPLETED(已完成)",
  `submitted_at` datetime NULL COMMENT "提交时间",
  `completed_at` datetime NULL COMMENT "完成时间",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "任务实例表";
-- Create "task_submission" table
CREATE TABLE `task_submission` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `item_id` bigint NOT NULL COMMENT "所属项目ID",
  `task_instance_id` bigint NOT NULL COMMENT "所属任务实例ID",
  `item_task_node_id` bigint NOT NULL COMMENT "关联项目任务节点ID",
  `submitter_id` bigint NOT NULL COMMENT "提交人ID",
  `metadata` json NULL COMMENT "提交元数据(JSON对象，按节点 meta_schema 组织)",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)",
  `reviewer_id` bigint NULL COMMENT "审核人ID（打回操作人）",
  `reviewed_at` datetime NULL COMMENT "审核/打回时间",
  `review_comment` varchar(500) NULL DEFAULT "" COMMENT "审核/打回意见",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "任务提交记录表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "任务模板表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "任务模板节点表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "Token黑名单表";
-- Create "user" table
CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT "用户ID",
  `username` varchar(50) NOT NULL COMMENT "用户名（登录用）",
  `password` varchar(100) NOT NULL COMMENT "密码（加密存储）",
  `nickname` varchar(50) NOT NULL DEFAULT "" COMMENT "昵称（展示用）",
  `email` varchar(100) NOT NULL DEFAULT "" COMMENT "邮箱（登录/通知）",
  `avatar_file_id` bigint NULL COMMENT "用户头像",
  `default_org_id` bigint NOT NULL DEFAULT 0 COMMENT "默认组织ID",
  `status` tinyint NOT NULL DEFAULT 1 COMMENT "状态 1-启用 0-禁用",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_email` (`email`) COMMENT "邮箱唯一",
  UNIQUE INDEX `uk_username` (`username`) COMMENT "用户名唯一"
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "系统用户表";
-- Create "user_operation_log" table
CREATE TABLE `user_operation_log` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT "操作人ID",
  `project_id` bigint NOT NULL COMMENT "所属企划ID",
  `item_id` bigint NULL COMMENT "所属项目ID",
  `operation_type` tinyint NOT NULL COMMENT "操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)",
  `request_body` json NULL COMMENT "操作请求体",
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "操作时间",
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "用户操作记录表";
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
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT "用户角色关联表";
