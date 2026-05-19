-- =====================================================
-- longlian-oa-backend v1.1.0 站内信模块
-- 数据库: MySQL
-- 创建时间: 2026-05-20
-- 包含模块: 站内消息、阅读记录
-- =====================================================

-- ----------------------------
-- 站内消息模块表
-- ----------------------------

-- 站内消息表 inbox_message
CREATE TABLE `inbox_message` (
    `id` bigint NOT NULL COMMENT '消息ID',
    `type` varchar(50) NOT NULL COMMENT '消息类型（PROJECT_UPDATE等）',
    `title` varchar(200) NOT NULL COMMENT '消息标题',
    `content` text NOT NULL COMMENT '消息内容',
    `target_type` tinyint NOT NULL COMMENT '目标类型 1-个人 2-组织',
    `target_id` bigint DEFAULT NULL COMMENT '目标ID（用户ID或组织ID）',
    `link_type` tinyint DEFAULT NULL COMMENT '链接类型 1-站内 2-站外',
    `link_value` varchar(500) DEFAULT NULL COMMENT '链接值（站内相对路径或站外URL）',
    `related_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型（如project/task）',
    `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_target_type_id` (`target_type`, `target_id`) COMMENT '目标查询索引',
    INDEX `idx_created_at` (`created_at`) COMMENT '时间排序索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- 站内消息阅读记录表 inbox_message_read
CREATE TABLE `inbox_message_read` (
    `id` bigint NOT NULL COMMENT '主键ID',
    `message_id` bigint NOT NULL COMMENT '消息ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `read_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_message` (`user_id`, `message_id`) COMMENT '用户-消息唯一',
    INDEX `idx_user_id` (`user_id`) COMMENT '用户查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息阅读记录表';
