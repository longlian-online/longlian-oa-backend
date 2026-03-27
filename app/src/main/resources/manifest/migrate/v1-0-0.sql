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
                              `id` bigint NOT NULL COMMENT '权限ID',
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
                        `id` bigint NOT NULL COMMENT '角色ID',
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
                                   `id` bigint NOT NULL COMMENT '主键ID',
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
                        `id` bigint NOT NULL COMMENT '用户ID',
                        `username` varchar(50) NOT NULL COMMENT '用户名（登录用）',
                        `password` varchar(100) NOT NULL COMMENT '密码（加密存储）',
                        `nickname` varchar(50) NOT NULL DEFAULT '' COMMENT '昵称（展示用）',
                        `email` varchar(100) NOT NULL DEFAULT '' COMMENT '邮箱（登录/通知）',
                        `avatar_file_id` bigint DEFAULT NULL COMMENT '用户头像',
                        `default_org_id` bigint NOT NULL DEFAULT 0 COMMENT '默认组织ID',
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
                             `id` bigint NOT NULL COMMENT '主键ID',
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
                                `id` bigint NOT NULL COMMENT '组织ID',
                                `name` varchar(100) NOT NULL COMMENT '组织名称',
                                `avatar_file_id` bigint DEFAULT NULL COMMENT '组织头像',
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
                                       `id` bigint NOT NULL,
                                       `org_id` bigint NOT NULL COMMENT '组织ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID',
                                       `org_role` varchar(20) NOT NULL DEFAULT 'ORG_USER' COMMENT '组织内角色：ORG_ADMIN/ORG_USER',
                                       `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入组时间',
                                       `last_submitted_at` datetime DEFAULT NULL COMMENT '上次提交任务时间',
                                       `submit_count` int NOT NULL DEFAULT 0 COMMENT '任务提交总数',
                                       `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                       `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       `deleted_at` datetime DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE,
                                       UNIQUE INDEX `uk_org_member_user`(`org_id`, `user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织成员表';

-- 2.3 入组申请表 group_application
CREATE TABLE `group_application` (
                                     `id` bigint NOT NULL,
                                     `org_id` bigint NOT NULL COMMENT '目标组织ID',
                                     `user_id` bigint NOT NULL COMMENT '申请人ID',
                                     `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待审核 1-通过 2-拒绝',
                                     `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
                                     `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
                                     `review_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
                                     `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     `deleted_at` datetime DEFAULT NULL,
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `uk_application_org_user`(`org_id`, `user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入组申请表';

-- ----------------------------
-- 3. 企划/项目管理模块表
-- ----------------------------

-- 3.1 企划表 project
CREATE TABLE `project` (
                           `id` bigint NOT NULL,
                           `org_id` bigint NOT NULL COMMENT '所属组织ID',
                           `type_id` bigint NOT NULL COMMENT '企划类型ID',
                           `title` varchar(100) NOT NULL COMMENT '企划名称',
                           `alias` varchar(100) DEFAULT '' COMMENT '别名',
                           `author` varchar(100) NOT NULL COMMENT '企划作者',
                           `cover_file_id` bigint DEFAULT NULL COMMENT '封面图',
                           `description` text COMMENT '简介',
                           `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-进行中 2-已完成 3-已归档',
                           `creator_id` bigint NOT NULL COMMENT '创建人ID',
                           `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           `deleted_at` datetime DEFAULT NULL,
                           PRIMARY KEY (`id`) USING BTREE,
                           INDEX `idx_org_type`(`org_id`, `type_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企划表';

-- 3.2 企划类型表 project_type
CREATE TABLE `project_type` (
                                `id` bigint NOT NULL COMMENT '企划类型ID',
                                `org_id` bigint NOT NULL COMMENT '所属组织ID',
                                `name` varchar(50) NOT NULL COMMENT '类型名称（如：漫画/小说/美术/视频）',
                                `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
                                `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                `creator_id` bigint NOT NULL COMMENT '创建人ID',
                                `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                `deleted_at` datetime DEFAULT NULL,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企划类型表';

-- 3.3 项目表 item
CREATE TABLE `item` (
                        `id` bigint NOT NULL,
                        `project_id` bigint NOT NULL COMMENT '所属企划ID',
                        `title` varchar(100) NOT NULL COMMENT '项目名称',
                        `task_template_id` bigint NOT NULL COMMENT '创建时关联的任务模板ID（仅记录溯源）',
                        `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-进行中 2-已完成 3-待发布',
                        `creator_id` bigint NOT NULL COMMENT '创建人ID',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `deleted_at` datetime DEFAULT NULL,
                        PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 3.4 原子任务表
CREATE TABLE `base_task` (
                             `id` bigint NOT NULL COMMENT '原子任务ID',
                             `org_id` bigint NOT NULL COMMENT '所属组织ID',
                             `name` varchar(100) NOT NULL COMMENT '任务名称（如：创建/翻译/校对）',
                             `icon_file_id` bigint DEFAULT NULL COMMENT '图标标识',
                             `description` varchar(500) DEFAULT '' COMMENT '任务说明',
                             `need_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否需要上传文件：0-否 1-是',
                             `required_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否必须上传：0-否 1-是',
                             `allowed_mime_types` varchar(500) DEFAULT '' COMMENT '允许的文件类型（逗号分隔）',
                             `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                             `creator_id` bigint NOT NULL COMMENT '创建人ID',
                             `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `deleted_at` datetime DEFAULT NULL,
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子任务表（最小任务单元）';

-- 3.5 任务模板表 task_template
CREATE TABLE `task_template` (
                                 `id` bigint NOT NULL COMMENT '任务模板ID',
                                 `org_id` bigint NOT NULL COMMENT '所属组织ID',
                                 `name` varchar(100) NOT NULL COMMENT '任务模板名称',
                                 `description` varchar(500) DEFAULT '' COMMENT '模板说明',
                                 `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
                                 `ref_count` int NOT NULL DEFAULT 0 COMMENT '关联的项目数',
                                 `creator_id` bigint NOT NULL COMMENT '创建人ID',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `deleted_at` datetime DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模板表';

-- 3.6 任务模板节点表 task_template_node
CREATE TABLE `task_template_node` (
                                      `id` bigint NOT NULL COMMENT '任务模板节点ID',
                                      `task_template_id` bigint NOT NULL COMMENT '所属任务模板ID',
                                      `base_task_id` bigint NOT NULL COMMENT '关联原子任务ID',
                                      `sort` int NOT NULL DEFAULT 0 COMMENT '步骤顺序',
                                      `parallel_sort` int DEFAULT NULL COMMENT '并行组号排序',
                                      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      `deleted_at` datetime DEFAULT NULL,
                                      PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模板节点表';

-- 3.7 项目任务流表 item_task_flow
CREATE TABLE `item_task_flow` (
                                  `id` bigint NOT NULL COMMENT '项目任务流ID',
                                  `item_id` bigint NOT NULL COMMENT '所属项目ID',
                                  `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                  `task_template_id` bigint NOT NULL COMMENT '关联任务模板ID',
                                  `name` varchar(100) NOT NULL COMMENT '任务流名称（继承自任务模板）',
                                  `description` varchar(500) DEFAULT '' COMMENT '任务流说明',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  `deleted_at` datetime DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `uk_item_id`(`item_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目任务流表';

-- 3.8 项目任务流节点表 item_task_node
CREATE TABLE `item_task_node` (
                                  `id` bigint NOT NULL COMMENT '项目任务节点ID',
                                  `item_task_flow_id` bigint NOT NULL COMMENT '所属项目任务流ID',
                                  `item_id` bigint NOT NULL COMMENT '所属项目ID',
                                  `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                  `base_task_id` bigint NOT NULL COMMENT '关联原子任务ID',
                                  `name` varchar(100) NOT NULL COMMENT '任务名称',
                                  `sort` int NOT NULL DEFAULT 0 COMMENT '步骤顺序',
                                  `parallel_sort` int DEFAULT NULL COMMENT '并行组号排序',
                                  `need_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否需要上传文件',
                                  `required_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否必须上传',
                                  `allowed_mime_types` varchar(500) DEFAULT '' COMMENT '允许的文件类型',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  `deleted_at` datetime DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_flow_sort`(`item_task_flow_id`, `sort`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目任务节点表';

-- 3.9 任务实例表 task_instance
CREATE TABLE `task_instance` (
                                 `id` bigint NOT NULL,
                                 `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                 `item_id` bigint NOT NULL COMMENT '所属项目ID',
                                 `base_task_id` bigint NOT NULL COMMENT '关联原子任务ID',
                                 `task_flow_id` bigint NOT NULL COMMENT '关联任务流ID',
                                 `assignee_id` bigint DEFAULT NULL COMMENT '接取人ID',
                                 `status` tinyint NOT NULL DEFAULT 1 COMMENT '任务状态 1-PENDING(待接取) 2-CLAIMED(已接取) 3-COMPLETED(已完成)',
                                 `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
                                 `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `deleted_at` datetime DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例表';

-- 3.10 任务提交记录表 task_submission
CREATE TABLE `task_submission` (
                                   `id` bigint NOT NULL,
                                   `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                   `item_id` bigint NOT NULL COMMENT '所属项目ID',
                                   `task_instance_id` bigint NOT NULL COMMENT '所属任务实例ID',
                                   `base_task_id` bigint NOT NULL COMMENT '关联原子任务ID',
                                   `submitter_id` bigint NOT NULL COMMENT '提交人ID',
                                   `file_id` bigint DEFAULT NULL COMMENT '上传文件ID',
                                   `metadata` json DEFAULT NULL COMMENT '元数据',
                                   `status` tinyint NOT NULL DEFAULT 1 COMMENT '提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)',
                                   `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID（打回操作人）',
                                   `reviewed_at` datetime DEFAULT NULL COMMENT '审核/打回时间',
                                   `review_comment` varchar(500) DEFAULT '' COMMENT '审核/打回意见',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `deleted_at` datetime DEFAULT NULL,
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务提交记录表';

-- 3.11 用户操作记录表 user_operation_log
CREATE TABLE `user_operation_log` (
                                      `id` bigint NOT NULL,
                                      `user_id` bigint NOT NULL COMMENT '操作人ID',
                                      `project_id` bigint NOT NULL COMMENT '所属企划ID',
                                      `item_id` bigint DEFAULT NULL COMMENT '所属项目ID',
                                      `operation_type` tinyint NOT NULL COMMENT '操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)',
                                      `request_body` json DEFAULT NULL COMMENT '操作请求体',
                                      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                      `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      `deleted_at` datetime DEFAULT NULL,
                                      PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作记录表';

-- 3.12 企划 - 工坊关联表 project_workshop
CREATE TABLE `project_workshop` (
                                    `id` bigint NOT NULL,
                                    `project_id` bigint NOT NULL COMMENT '企划ID',
                                    `user_id` bigint NOT NULL COMMENT '添加人ID',
                                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    `deleted_at` datetime DEFAULT NULL,
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企划-工坊关联表（用户添加企划）';


-- 4.1 文件存储表 file_storage

CREATE TABLE `file_storage` (
                                `id` bigint NOT NULL COMMENT '文件ID',
                                `org_id` bigint NOT NULL COMMENT '所属组织ID',
                                `storage_type` tinyint NOT NULL COMMENT '存储类型 1-本地存储 2-阿里云OSS 3-腾讯云COS',
                                `storage_key` varchar(255) NOT NULL COMMENT '存储唯一标识（如OSS的objectKey/本地文件路径）',
                                `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
                                `file_ext` varchar(20) NOT NULL COMMENT '文件扩展名',
                                `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小',
                                `file_mime` varchar(100) DEFAULT '' COMMENT '文件MIME类型',
                                `biz_type` varchar(50) NOT NULL COMMENT '业务类型（如：avatar/cover/task_submit）',
                                `biz_id` bigint NOT NULL COMMENT '业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）',
                                `process_status` tinyint NOT NULL DEFAULT 0 COMMENT '文件处理状态 0-未处理 1-处理中 2-已压缩 3-处理失败',
                                `is_referenced` tinyint NOT NULL DEFAULT 1 COMMENT '是否被引用 1-是 0-否（清理无用文件）',
                                `creator_id` bigint NOT NULL COMMENT '上传人ID',
                                `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                `deleted_at` datetime DEFAULT NULL,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用文件存储表';