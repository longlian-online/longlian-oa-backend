-- =====================================================
-- longlian-oa-backend v1.0.0 数据库
-- 数据库: MySQL
-- 创建时间: 2026-03-06
-- 包含模块: RBAC权限、组织管理、企划项目、任务管理
-- =====================================================

-- ----------------------------
-- 1. RBAC权限模块表
-- ----------------------------

-- 1.1 权限表 permission
CREATE TABLE `permission` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                              `perm_code` varchar(100) NOT NULL COMMENT '权限编码（如：org:member:disable）',
                              `perm_name` varchar(50) NOT NULL COMMENT '权限名称',
                              `perm_type` tinyint NOT NULL COMMENT '类型 1-菜单 2-按钮 3-接口',
                              `path` varchar(200) DEFAULT '' COMMENT '前端路由/接口路径',
                              `parent_id` bigint DEFAULT 0 COMMENT '父权限ID（用于菜单层级）',
                              `sort` int DEFAULT 0 COMMENT '排序（前端展示顺序）',
                              `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                              `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE INDEX `uk_perm_code`(`perm_code`) USING BTREE COMMENT '权限编码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 1.2 角色表 role
CREATE TABLE `role` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                        `role_code` varchar(50) NOT NULL COMMENT '角色编码（如：ORG_ADMIN）',
                        `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                        `description` varchar(200) DEFAULT '' COMMENT '角色描述',
                        `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `deleted_at` datetime DEFAULT NULL,
                        PRIMARY KEY (`id`) USING BTREE,
                        UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE COMMENT '角色编码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.3 角色权限关联表 role_permission
CREATE TABLE `role_permission` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `role_id` bigint NOT NULL COMMENT '角色ID',
                                   `permission_id` bigint NOT NULL COMMENT '权限ID',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `deleted_at` datetime DEFAULT NULL,
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_role_perm`(`role_id`, `permission_id`) USING BTREE COMMENT '角色-权限唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 1.4 系统用户表 user
CREATE TABLE `user` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                        `username` varchar(50) NOT NULL COMMENT '用户名（登录用）',
                        `password` varchar(100) NOT NULL COMMENT '密码（加密存储）',
                        `nickname` varchar(50) DEFAULT '' COMMENT '昵称（展示用）',
                        `email` varchar(100) DEFAULT '' COMMENT '邮箱（登录/通知）',
                        `avatar` varchar(255) DEFAULT '' COMMENT '用户头像',
                        `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `deleted_at` datetime DEFAULT NULL,
                        PRIMARY KEY (`id`) USING BTREE,
                        UNIQUE INDEX `uk_username`(`username`) USING BTREE COMMENT '用户名唯一',
                        UNIQUE INDEX `uk_email`(`email`) USING BTREE COMMENT '邮箱唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 1.5 用户角色关联表 user_role
CREATE TABLE `user_role` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `user_id` bigint NOT NULL COMMENT '用户ID',
                             `role_id` bigint NOT NULL COMMENT '角色ID',
                             `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `deleted_at` datetime DEFAULT NULL,
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE COMMENT '用户-角色唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 2. 组织管理模块表
-- ----------------------------

-- 2.1 组织表 organization
CREATE TABLE `organization` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组织ID',
                                `name` varchar(100) NOT NULL COMMENT '组织名称',
                                `avatar` varchar(255) DEFAULT '' COMMENT '组织头像',
                                `description` varchar(500) DEFAULT '' COMMENT '组织简介',
                                `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                `creator_id` bigint NOT NULL COMMENT '创建人ID',
                                `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                `deleted_at` datetime DEFAULT NULL,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

-- 2.2 组织成员表 organization_member
CREATE TABLE `organization_member` (
                                       `id` bigint NOT NULL AUTO_INCREMENT,
                                       `org_id` bigint NOT NULL COMMENT '组织ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID',
                                       `org_role` varchar(20) NOT NULL DEFAULT 'ORG_USER' COMMENT '组织内角色：ORG_ADMIN/ORG_USER',
                                       `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入组时间',
                                       `last_submit_time` datetime DEFAULT NULL COMMENT '上次提交任务时间',
                                       `submit_count` int NOT NULL DEFAULT 0 COMMENT '任务提交总数',
                                       `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                       `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       `deleted_at` datetime DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE,
                                       UNIQUE INDEX `uk_org_user`(`org_id`, `user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织成员表';

-- 2.3 入组申请表 group_application
CREATE TABLE `group_application` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `org_id` bigint NOT NULL COMMENT '目标组织ID',
                                     `user_id` bigint NOT NULL COMMENT '申请人ID',
                                     `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待审核 1-通过 2-拒绝',
                                     `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
                                     `review_time` datetime DEFAULT NULL COMMENT '审核时间',
                                     `review_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
                                     `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     `deleted_at` datetime DEFAULT NULL,
                                     PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入组申请表';

-- ----------------------------
-- 3. 企划/项目管理模块表
-- ----------------------------

-- 3.1 企划表 project
CREATE TABLE `project` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `org_id` bigint NOT NULL COMMENT '所属组织ID',
                           `title` varchar(100) NOT NULL COMMENT '企划名称',
                           `alias` varchar(100) DEFAULT '' COMMENT '别名',
                           `type` varchar(20) NOT NULL COMMENT '类型：MANGA/NOVEL/ART/VIDEO',
                           `cover` varchar(255) DEFAULT '' COMMENT '封面图',
                           `original_author` varchar(50) DEFAULT '' COMMENT '原作者',
                           `description` text COMMENT '简介',
                           `status` varchar(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/COMPLETED/ARCHIVED',
                           `creator_id` bigint NOT NULL COMMENT '创建人ID',
                           `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           `deleted_at` datetime DEFAULT NULL,
                           PRIMARY KEY (`id`) USING BTREE,
                           INDEX `idx_org_type`(`org_id`, `type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企划表';

-- 3.2 项目表 item
CREATE TABLE `item` (
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `project_id` bigint NOT NULL COMMENT '所属企划ID',
                        `title` varchar(100) NOT NULL COMMENT '项目名称',
                        `flow_template_id` bigint NOT NULL COMMENT '关联流程模板ID',
                        `status` varchar(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/COMPLETED/PENDING_PUBLISH',
                        `progress` int NOT NULL DEFAULT 0 COMMENT '进度百分比（0-100）',
                        `creator_id` bigint NOT NULL COMMENT '创建人ID',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `deleted_at` datetime DEFAULT NULL,
                        PRIMARY KEY (`id`) USING BTREE,
                        INDEX `idx_project_status`(`project_id`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 3.3 流程模板表 flow_template
CREATE TABLE `flow_template` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `org_id` bigint NOT NULL COMMENT '所属组织ID（模板归属）',
                                 `name` varchar(100) NOT NULL COMMENT '流程模板名称',
                                 `description` varchar(500) DEFAULT '' COMMENT '简介',
                                 `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                 `ref_count` int NOT NULL DEFAULT 0 COMMENT '引用次数',
                                 `creator_id` bigint NOT NULL COMMENT '创建人ID',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `deleted_at` datetime DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE,
                                 INDEX `idx_org_ref`(`org_id`, `ref_count`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板表';

-- 3.4 企划 - 工坊关联表 project_workshop
CREATE TABLE `project_workshop` (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `project_id` bigint NOT NULL COMMENT '企划ID',
                                    `user_id` bigint NOT NULL COMMENT '添加人ID',
                                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加到工坊时间（创建时间）',
                                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    `deleted_at` datetime DEFAULT NULL,
                                    PRIMARY KEY (`id`) USING BTREE,
                                    UNIQUE INDEX `uk_project_user`(`project_id`, `user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企划-工坊关联表（用户添加企划）';

-- ----------------------------
-- 4. 任务管理模块表
-- ----------------------------

-- 4.1 任务模板表 task_template
CREATE TABLE `task_template` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `flow_id` bigint NOT NULL COMMENT '所属流程模板ID',
                                 `name` varchar(100) NOT NULL COMMENT '任务名称（如：翻译/校对）',
                                 `icon` varchar(50) DEFAULT '' COMMENT '图标标识',
                                 `description` varchar(500) DEFAULT '' COMMENT '简介',
                                 `sort` int NOT NULL DEFAULT 0 COMMENT '步骤顺序',
                                 `parallel_group` int DEFAULT NULL COMMENT '并行组号（同组可并行）',
                                 `need_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否需要上传文件：0-否 1-是',
                                 `required_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否必须上传：0-否 1-是',
                                 `allowed_mime_types` varchar(500) DEFAULT '' COMMENT '允许的文件类型（逗号分隔）',
                                 `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `deleted_at` datetime DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE,
                                 INDEX `idx_flow_sort`(`flow_id`, `sort`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模板表';

-- 4.2 任务实例表 task_instance
CREATE TABLE `task_instance` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                 `item_id` bigint NOT NULL COMMENT '所属项目ID',
                                 `task_template_id` bigint NOT NULL COMMENT '关联任务模板ID',
                                 `assignee_id` bigint DEFAULT NULL COMMENT '接取人ID',
                                 `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CLAIMED/SUBMITTED/COMPLETED/REJECTED',
                                 `reject_reason` varchar(500) DEFAULT '' COMMENT '打回原因',
                                 `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
                                 `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `deleted_at` datetime DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE,
                                 INDEX `idx_project_status`(`project_id`, `status`) USING BTREE COMMENT '高频查询：按企划+任务状态筛选',
                                 INDEX `idx_item_status`(`item_id`, `status`) USING BTREE,
                                 INDEX `idx_assignee`(`assignee_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例表';

-- 4.3 任务提交记录表 task_submission
CREATE TABLE `task_submission` (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                   `task_instance_id` bigint NOT NULL COMMENT '所属任务实例ID',
                                   `submitter_id` bigint NOT NULL COMMENT '提交人ID',
                                   `file_url` varchar(255) DEFAULT '' COMMENT '上传文件地址',
                                   `metadata` json DEFAULT NULL COMMENT '元数据（JSON格式）',
                                   `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/APPROVED/REJECTED',
                                   `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
                                   `review_time` datetime DEFAULT NULL COMMENT '审核时间',
                                   `review_comment` varchar(500) DEFAULT '' COMMENT '审核意见',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `deleted_at` datetime DEFAULT NULL,
                                   PRIMARY KEY (`id`) USING BTREE,
                                   INDEX `idx_project_status`(`project_id`, `status`) USING BTREE COMMENT '高频查询：按企划+提交状态筛选',
                                   INDEX `idx_task_status`(`task_instance_id`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务提交记录表';