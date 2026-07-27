-- ============================================================
-- PostgreSQL Schema (唯一真实来源)
-- 由 Atlas 声明式管理，勿手动执行
-- ============================================================

-- 通用 trigger 函数：自动更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create "admin" table
CREATE TABLE admin (
  id bigint NOT NULL,
  username varchar(64) NOT NULL,
  password varchar(128) NOT NULL,
  role varchar(32) NOT NULL,
  last_login_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_admin_username ON admin (username);
COMMENT ON TABLE admin IS '管理员表';
COMMENT ON COLUMN admin.username IS '用户名';
COMMENT ON COLUMN admin.password IS '密码';
COMMENT ON COLUMN admin.role IS '角色：root/normal';
COMMENT ON COLUMN admin.last_login_at IS '上一次登录时间';

-- Create "base_task" table
CREATE TABLE base_task (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  icon_file_id bigint NULL,
  description varchar(500) NULL DEFAULT '',
  meta_schema json NULL,
  status smallint NOT NULL DEFAULT 1,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE base_task IS '原子任务表（最小任务单元）';
COMMENT ON COLUMN base_task.id IS '原子任务ID';
COMMENT ON COLUMN base_task.org_id IS '所属组织ID';
COMMENT ON COLUMN base_task.name IS '任务名称（标题）（如：创建/翻译/校对）';
COMMENT ON COLUMN base_task.icon_file_id IS '图标标识';
COMMENT ON COLUMN base_task.description IS '任务说明（简介）';
COMMENT ON COLUMN base_task.meta_schema IS '元数据字段定义(JSON数组)';
COMMENT ON COLUMN base_task.status IS '状态 1-启用 0-禁用';
COMMENT ON COLUMN base_task.creator_id IS '创建人ID';

-- Create "email_verify_otp" table
CREATE TABLE email_verify_otp (
  id bigint NOT NULL,
  otp_id bigint NOT NULL,
  receiver varchar(255) NOT NULL,
  business_type smallint NOT NULL DEFAULT 0,
  send_status smallint NOT NULL DEFAULT 0,
  sent_at timestamp NULL,
  failed_at timestamp NULL,
  fail_reason varchar(500) NOT NULL DEFAULT '',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_receiver_send_status_created_at ON email_verify_otp (receiver, send_status, created_at);
COMMENT ON TABLE email_verify_otp IS '邮箱验证码扩展表';
COMMENT ON COLUMN email_verify_otp.id IS '邮箱验证码ID';
COMMENT ON COLUMN email_verify_otp.otp_id IS '关联验证码ID';
COMMENT ON COLUMN email_verify_otp.receiver IS '接收者邮箱';
COMMENT ON COLUMN email_verify_otp.business_type IS '业务类型 0-登录 1-注册 2-忘记密码';
COMMENT ON COLUMN email_verify_otp.send_status IS '发送状态 0-待发送 1-发送成功 2-发送失败';
COMMENT ON COLUMN email_verify_otp.sent_at IS '发送成功时间';
COMMENT ON COLUMN email_verify_otp.failed_at IS '发送失败时间';
COMMENT ON COLUMN email_verify_otp.fail_reason IS '发送失败原因';

-- Create "group_application" table
CREATE TABLE group_application (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  user_id bigint NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  reviewer_id bigint NULL,
  reviewed_at timestamp NULL,
  review_remark varchar(500) NULL DEFAULT '',
  application_type smallint NULL,
  username varchar(50) NULL,
  password varchar(100) NULL,
  nickname varchar(50) NULL DEFAULT '',
  email varchar(100) NULL DEFAULT '',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE group_application IS '入组申请表';
COMMENT ON COLUMN group_application.org_id IS '目标组织ID';
COMMENT ON COLUMN group_application.user_id IS '申请人ID';
COMMENT ON COLUMN group_application.status IS '状态：0-待审核 1-通过 2-拒绝';
COMMENT ON COLUMN group_application.reviewer_id IS '审核人ID';
COMMENT ON COLUMN group_application.reviewed_at IS '审核时间';
COMMENT ON COLUMN group_application.review_remark IS '审核备注';
COMMENT ON COLUMN group_application.application_type IS '申请入组的类型：0-注册入组 1-已注册用户入组';
COMMENT ON COLUMN group_application.username IS '用户名';
COMMENT ON COLUMN group_application.password IS '密码';
COMMENT ON COLUMN group_application.nickname IS '昵称';
COMMENT ON COLUMN group_application.email IS '邮箱';

-- Create "item" table
CREATE TABLE item (
  id bigint NOT NULL,
  project_id bigint NOT NULL,
  title varchar(100) NOT NULL,
  task_template_id bigint NOT NULL,
  status smallint NOT NULL DEFAULT 1,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE item IS '项目表';
COMMENT ON COLUMN item.project_id IS '所属企划ID';
COMMENT ON COLUMN item.title IS '项目名称';
COMMENT ON COLUMN item.task_template_id IS '创建时关联的任务模板ID（仅记录溯源）';
COMMENT ON COLUMN item.status IS '状态 1-进行中 2-已完成 3-已公布';
COMMENT ON COLUMN item.creator_id IS '创建人ID';

-- Create "item_task_flow" table
CREATE TABLE item_task_flow (
  id bigint NOT NULL,
  item_id bigint NOT NULL,
  project_id bigint NOT NULL,
  task_template_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  description varchar(500) NULL DEFAULT '',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_item_task_flow_item_id ON item_task_flow (item_id);
COMMENT ON TABLE item_task_flow IS '项目任务流表';
COMMENT ON COLUMN item_task_flow.id IS '项目任务流ID';
COMMENT ON COLUMN item_task_flow.item_id IS '所属项目ID';
COMMENT ON COLUMN item_task_flow.project_id IS '所属企划ID';
COMMENT ON COLUMN item_task_flow.task_template_id IS '关联任务模板ID';
COMMENT ON COLUMN item_task_flow.name IS '任务流名称（继承自任务模板）';
COMMENT ON COLUMN item_task_flow.description IS '任务流说明';

-- Create "item_task_node" table
CREATE TABLE item_task_node (
  id bigint NOT NULL,
  item_task_flow_id bigint NOT NULL,
  item_id bigint NOT NULL,
  project_id bigint NOT NULL,
  base_task_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  meta_schema json NULL,
  sort int NOT NULL DEFAULT 0,
  parallel_sort int NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_flow_sort ON item_task_node (item_task_flow_id, sort);
COMMENT ON TABLE item_task_node IS '项目任务节点表';
COMMENT ON COLUMN item_task_node.id IS '项目任务节点ID';
COMMENT ON COLUMN item_task_node.item_task_flow_id IS '所属项目任务流ID';
COMMENT ON COLUMN item_task_node.item_id IS '所属项目ID';
COMMENT ON COLUMN item_task_node.project_id IS '所属企划ID';
COMMENT ON COLUMN item_task_node.base_task_id IS '关联原子任务ID';
COMMENT ON COLUMN item_task_node.name IS '任务名称';
COMMENT ON COLUMN item_task_node.meta_schema IS '节点元数据字段定义快照(JSON数组)';
COMMENT ON COLUMN item_task_node.sort IS '步骤顺序';
COMMENT ON COLUMN item_task_node.parallel_sort IS '并行组号排序';

-- Create "one_time_password" table
CREATE TABLE one_time_password (
  id bigint NOT NULL,
  code varchar(128) NOT NULL,
  expired_at timestamp NOT NULL,
  used_at timestamp NULL,
  biz_type smallint NOT NULL,
  status smallint NOT NULL,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_code_expired_at ON one_time_password (code, expired_at);
COMMENT ON TABLE one_time_password IS '一次性密码（otp）表';
COMMENT ON COLUMN one_time_password.code IS '验证码';
COMMENT ON COLUMN one_time_password.expired_at IS '过期时间';
COMMENT ON COLUMN one_time_password.used_at IS '使用时间';
COMMENT ON COLUMN one_time_password.biz_type IS '业务类型 1-邀请创建组织 2-邀请加入组织 3-邮箱验证码';
COMMENT ON COLUMN one_time_password.status IS '状态 0-待使用 1-已使用';
COMMENT ON COLUMN one_time_password.creator_id IS '创建者 ID';

-- Create "organization" table
CREATE TABLE organization (
  id bigint NOT NULL,
  name varchar(100) NOT NULL,
  avatar_file_id bigint NULL,
  description varchar(500) NULL DEFAULT '',
  status smallint NOT NULL DEFAULT 1,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE organization IS '组织表';
COMMENT ON COLUMN organization.id IS '组织ID';
COMMENT ON COLUMN organization.name IS '组织名称';
COMMENT ON COLUMN organization.avatar_file_id IS '组织头像';
COMMENT ON COLUMN organization.description IS '组织简介';
COMMENT ON COLUMN organization.status IS '状态 1-启用 0-禁用';
COMMENT ON COLUMN organization.creator_id IS '创建人ID';

-- Create "organization_create_otp" table
CREATE TABLE organization_create_otp (
  id bigint NOT NULL,
  otp_id bigint NOT NULL,
  invited_user_id bigint NULL,
  org_id bigint NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE organization_create_otp IS '邀请创建组织表';
COMMENT ON COLUMN organization_create_otp.id IS '邀请ID';
COMMENT ON COLUMN organization_create_otp.otp_id IS '关联验证码ID';
COMMENT ON COLUMN organization_create_otp.invited_user_id IS '被邀请用户ID';
COMMENT ON COLUMN organization_create_otp.org_id IS '创建成功后的组织ID';

-- Create "organization_join_otp" table
CREATE TABLE organization_join_otp (
  id bigint NOT NULL,
  otp_id bigint NOT NULL,
  org_id bigint NOT NULL,
  invited_user_id bigint NULL,
  org_member_id bigint NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE organization_join_otp IS '邀请加入组织表';
COMMENT ON COLUMN organization_join_otp.id IS '邀请ID';
COMMENT ON COLUMN organization_join_otp.otp_id IS '关联验证码ID';
COMMENT ON COLUMN organization_join_otp.org_id IS '目标组织ID';
COMMENT ON COLUMN organization_join_otp.invited_user_id IS '被邀请用户ID';
COMMENT ON COLUMN organization_join_otp.org_member_id IS '加入成功后的组织成员ID';

-- Create "organization_member" table
CREATE TABLE organization_member (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  user_id bigint NOT NULL,
  org_role varchar(20) NOT NULL DEFAULT 'ORG_USER',
  joined_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_submitted_at timestamp NULL,
  submit_count int NOT NULL DEFAULT 0,
  status smallint NOT NULL DEFAULT 1,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_org_member_user ON organization_member (org_id, user_id);
COMMENT ON TABLE organization_member IS '组织成员表';
COMMENT ON COLUMN organization_member.org_id IS '组织ID';
COMMENT ON COLUMN organization_member.user_id IS '用户ID';
COMMENT ON COLUMN organization_member.org_role IS '组织内角色：ORG_ADMIN/ORG_USER';
COMMENT ON COLUMN organization_member.joined_at IS '入组时间';
COMMENT ON COLUMN organization_member.last_submitted_at IS '上次提交任务时间';
COMMENT ON COLUMN organization_member.submit_count IS '任务提交总数';
COMMENT ON COLUMN organization_member.status IS '状态 1-启用 0-禁用';

-- Create "permission" table
CREATE TABLE permission (
  id bigint NOT NULL,
  perm_code varchar(100) NOT NULL,
  perm_name varchar(50) NOT NULL,
  perm_type smallint NOT NULL,
  path varchar(200) NULL DEFAULT '',
  parent_id bigint NULL DEFAULT 0,
  sort int NULL DEFAULT 0,
  status smallint NOT NULL DEFAULT 1,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_perm_code ON permission (perm_code);
COMMENT ON TABLE permission IS '权限表';
COMMENT ON COLUMN permission.id IS '权限ID';
COMMENT ON COLUMN permission.perm_code IS '权限编码（如：org:member:disable）';
COMMENT ON COLUMN permission.perm_name IS '权限名称';
COMMENT ON COLUMN permission.perm_type IS '类型 1-菜单 2-按钮 3-接口';
COMMENT ON COLUMN permission.path IS '前端路由/接口路径';
COMMENT ON COLUMN permission.parent_id IS '父权限ID（用于菜单层级）';
COMMENT ON COLUMN permission.sort IS '排序（前端展示顺序）';
COMMENT ON COLUMN permission.status IS '状态 1-启用 0-禁用';
COMMENT ON COLUMN permission.created_at IS '创建时间';
COMMENT ON COLUMN permission.updated_at IS '更新时间';
COMMENT ON COLUMN permission.deleted_at IS '软删除时间';

-- Create "project" table
CREATE TABLE project (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  type_id bigint NOT NULL,
  title varchar(100) NOT NULL,
  alias varchar(100) NULL DEFAULT '',
  metadata json NULL,
  cover_file_id bigint NULL,
  description text NULL,
  status smallint NOT NULL DEFAULT 1,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_org_type ON project (org_id, type_id);
COMMENT ON TABLE project IS '企划表';
COMMENT ON COLUMN project.org_id IS '所属组织ID';
COMMENT ON COLUMN project.type_id IS '企划类型ID';
COMMENT ON COLUMN project.title IS '企划名称';
COMMENT ON COLUMN project.alias IS '别名';
COMMENT ON COLUMN project.metadata IS '扩展信息(JSON字符串)';
COMMENT ON COLUMN project.cover_file_id IS '封面图';
COMMENT ON COLUMN project.description IS '简介';
COMMENT ON COLUMN project.status IS '状态 1-进行中 2-已完成 3-已归档';
COMMENT ON COLUMN project.creator_id IS '创建人ID';

-- Create "project_type" table
CREATE TABLE project_type (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  name varchar(50) NOT NULL,
  status smallint NOT NULL DEFAULT 1,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE project_type IS '企划类型表';
COMMENT ON COLUMN project_type.id IS '企划类型ID';
COMMENT ON COLUMN project_type.org_id IS '所属组织ID';
COMMENT ON COLUMN project_type.name IS '类型名称（如：漫画/小说/美术/视频）';
COMMENT ON COLUMN project_type.status IS '状态 1-启用 0-禁用';
COMMENT ON COLUMN project_type.creator_id IS '创建人ID';

-- Create "project_workshop" table
CREATE TABLE project_workshop (
  id bigint NOT NULL,
  project_id bigint NOT NULL,
  user_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE project_workshop IS '企划-工坊关联表（用户添加企划）';
COMMENT ON COLUMN project_workshop.project_id IS '企划ID';
COMMENT ON COLUMN project_workshop.user_id IS '添加人ID';

-- Create "resource" table
CREATE TABLE resource (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  storage_type smallint NOT NULL,
  storage_key varchar(255) NOT NULL,
  file_name varchar(255) NOT NULL,
  file_ext varchar(20) NOT NULL,
  file_size bigint NOT NULL DEFAULT 0,
  file_mime varchar(100) NULL DEFAULT '',
  biz_type varchar(50) NOT NULL,
  biz_id bigint NOT NULL,
  process_status smallint NOT NULL DEFAULT 0,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE resource IS '通用文件存储表';
COMMENT ON COLUMN resource.id IS '文件ID';
COMMENT ON COLUMN resource.org_id IS '所属组织ID';
COMMENT ON COLUMN resource.storage_type IS '存储类型 1-本地存储 2-云对象存储';
COMMENT ON COLUMN resource.storage_key IS '存储唯一标识（如OSS的objectKey/本地文件路径）';
COMMENT ON COLUMN resource.file_name IS '原始文件名';
COMMENT ON COLUMN resource.file_ext IS '文件扩展名';
COMMENT ON COLUMN resource.file_size IS '文件大小';
COMMENT ON COLUMN resource.file_mime IS '文件MIME类型';
COMMENT ON COLUMN resource.biz_type IS '业务类型（如：avatar/cover/task_submit）';
COMMENT ON COLUMN resource.biz_id IS '业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）';
COMMENT ON COLUMN resource.process_status IS '状态 0-未上传 1-已上传 3-已废弃';
COMMENT ON COLUMN resource.creator_id IS '上传人ID';

-- Create "role" table
CREATE TABLE role (
  id bigint NOT NULL,
  role_code varchar(50) NOT NULL,
  role_name varchar(50) NOT NULL,
  description varchar(200) NULL DEFAULT '',
  status smallint NOT NULL DEFAULT 1,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_role_code ON role (role_code);
COMMENT ON TABLE role IS '角色表';
COMMENT ON COLUMN role.id IS '角色ID';
COMMENT ON COLUMN role.role_code IS '角色编码（如：ORG_ADMIN）';
COMMENT ON COLUMN role.role_name IS '角色名称';
COMMENT ON COLUMN role.description IS '角色描述';
COMMENT ON COLUMN role.status IS '状态 1-启用 0-禁用';

-- Create "role_permission" table
CREATE TABLE role_permission (
  id bigint NOT NULL,
  role_id bigint NOT NULL,
  permission_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_role_perm ON role_permission (role_id, permission_id);
COMMENT ON TABLE role_permission IS '角色权限关联表';
COMMENT ON COLUMN role_permission.id IS '主键ID';
COMMENT ON COLUMN role_permission.role_id IS '角色ID';
COMMENT ON COLUMN role_permission.permission_id IS '权限ID';

-- Create "scheduled_task_log" table
CREATE TABLE scheduled_task_log (
  id bigint NOT NULL,
  task_name varchar(100) NOT NULL,
  trigger_source smallint NOT NULL,
  execute_time_param timestamp NOT NULL,
  status smallint NOT NULL,
  error_message text NULL,
  execution_id varchar(64) NOT NULL,
  triggered_by bigint NULL,
  started_at timestamp NOT NULL,
  ended_at timestamp NULL,
  duration_ms bigint NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_started_at ON scheduled_task_log (started_at);
CREATE INDEX idx_task_name ON scheduled_task_log (task_name);
COMMENT ON TABLE scheduled_task_log IS '定时任务执行日志表';
COMMENT ON COLUMN scheduled_task_log.id IS '日志ID';
COMMENT ON COLUMN scheduled_task_log.task_name IS '任务名称';
COMMENT ON COLUMN scheduled_task_log.trigger_source IS '触发来源 1-SCHEDULED(定时触发) 2-MANUAL(手动触发)';
COMMENT ON COLUMN scheduled_task_log.execute_time_param IS '上层传递的执行时间';
COMMENT ON COLUMN scheduled_task_log.status IS '执行状态 1-RUNNING(执行中) 2-SUCCESS(成功) 3-FAILED(失败)';
COMMENT ON COLUMN scheduled_task_log.error_message IS '失败时的错误信息';
COMMENT ON COLUMN scheduled_task_log.execution_id IS '执行追踪ID';
COMMENT ON COLUMN scheduled_task_log.triggered_by IS '手动触发人ID（定时触发时为NULL）';
COMMENT ON COLUMN scheduled_task_log.started_at IS '开始执行时间';
COMMENT ON COLUMN scheduled_task_log.ended_at IS '结束执行时间';
COMMENT ON COLUMN scheduled_task_log.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN scheduled_task_log.created_at IS '创建时间';
COMMENT ON COLUMN scheduled_task_log.updated_at IS '更新时间';
COMMENT ON COLUMN scheduled_task_log.deleted_at IS '软删除时间';

-- Create "task_instance" table
CREATE TABLE task_instance (
  id bigint NOT NULL,
  project_id bigint NOT NULL,
  item_id bigint NOT NULL,
  item_task_node_id bigint NOT NULL,
  task_flow_id bigint NOT NULL,
  assignee_id bigint NULL,
  status smallint NOT NULL DEFAULT 1,
  submitted_at timestamp NULL,
  completed_at timestamp NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE task_instance IS '任务实例表';
COMMENT ON COLUMN task_instance.project_id IS '所属企划ID';
COMMENT ON COLUMN task_instance.item_id IS '所属项目ID';
COMMENT ON COLUMN task_instance.item_task_node_id IS '关联项目任务节点ID';
COMMENT ON COLUMN task_instance.task_flow_id IS '关联任务流ID';
COMMENT ON COLUMN task_instance.assignee_id IS '接取人ID';
COMMENT ON COLUMN task_instance.status IS '任务状态 1-PENDING(待接取) 2-CLAIMED(待提交) 3-COMPLETED(已完成)';
COMMENT ON COLUMN task_instance.submitted_at IS '提交时间';
COMMENT ON COLUMN task_instance.completed_at IS '完成时间';

-- Create "task_submission" table
CREATE TABLE task_submission (
  id bigint NOT NULL,
  project_id bigint NOT NULL,
  item_id bigint NOT NULL,
  task_instance_id bigint NOT NULL,
  item_task_node_id bigint NOT NULL,
  submitter_id bigint NOT NULL,
  metadata json NULL,
  status smallint NOT NULL DEFAULT 1,
  reviewer_id bigint NULL,
  reviewed_at timestamp NULL,
  review_comment varchar(500) NULL DEFAULT '',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE task_submission IS '任务提交记录表';
COMMENT ON COLUMN task_submission.project_id IS '所属企划ID';
COMMENT ON COLUMN task_submission.item_id IS '所属项目ID';
COMMENT ON COLUMN task_submission.task_instance_id IS '所属任务实例ID';
COMMENT ON COLUMN task_submission.item_task_node_id IS '关联项目任务节点ID';
COMMENT ON COLUMN task_submission.submitter_id IS '提交人ID';
COMMENT ON COLUMN task_submission.metadata IS '提交元数据(JSON对象，按节点 meta_schema 组织)';
COMMENT ON COLUMN task_submission.status IS '提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)';
COMMENT ON COLUMN task_submission.reviewer_id IS '审核人ID（打回操作人）';
COMMENT ON COLUMN task_submission.reviewed_at IS '审核/打回时间';
COMMENT ON COLUMN task_submission.review_comment IS '审核/打回意见';

-- Create "task_template" table
CREATE TABLE task_template (
  id bigint NOT NULL,
  org_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  description varchar(500) NULL DEFAULT '',
  status smallint NOT NULL DEFAULT 1,
  scope smallint NOT NULL DEFAULT 1,
  ref_count int NOT NULL DEFAULT 0,
  creator_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE task_template IS '任务模板表';
COMMENT ON COLUMN task_template.id IS '任务模板ID';
COMMENT ON COLUMN task_template.org_id IS '所属组织ID';
COMMENT ON COLUMN task_template.name IS '任务模板名称';
COMMENT ON COLUMN task_template.description IS '模板说明';
COMMENT ON COLUMN task_template.status IS '状态 1-启用 0-禁用';
COMMENT ON COLUMN task_template.scope IS '1-个人模板 2-组织通用模板';
COMMENT ON COLUMN task_template.ref_count IS '关联的项目数';
COMMENT ON COLUMN task_template.creator_id IS '创建人ID';

-- Create "task_template_node" table
CREATE TABLE task_template_node (
  id bigint NOT NULL,
  task_template_id bigint NOT NULL,
  base_task_id bigint NOT NULL,
  sort int NOT NULL DEFAULT 0,
  parallel_sort int NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE task_template_node IS '任务模板节点表';
COMMENT ON COLUMN task_template_node.id IS '任务模板节点ID';
COMMENT ON COLUMN task_template_node.task_template_id IS '所属任务模板ID';
COMMENT ON COLUMN task_template_node.base_task_id IS '关联原子任务ID';
COMMENT ON COLUMN task_template_node.sort IS '步骤顺序';
COMMENT ON COLUMN task_template_node.parallel_sort IS '并行组号排序';

-- Create "token_blacklist" table
CREATE TABLE token_blacklist (
  id bigint NOT NULL,
  token varchar(512) NOT NULL,
  token_type smallint NOT NULL,
  user_id bigint NOT NULL,
  reason varchar(200) NULL DEFAULT '',
  expired_at timestamp NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_expired ON token_blacklist (expired_at);
CREATE INDEX idx_user ON token_blacklist (token_type, user_id);
CREATE UNIQUE INDEX uk_token ON token_blacklist (token);
COMMENT ON TABLE token_blacklist IS 'Token黑名单表';
COMMENT ON COLUMN token_blacklist.id IS '主键ID';
COMMENT ON COLUMN token_blacklist.token IS 'Token字符串';
COMMENT ON COLUMN token_blacklist.token_type IS 'Token类型 1-用户端 2-管理端';
COMMENT ON COLUMN token_blacklist.user_id IS '用户/管理员ID';
COMMENT ON COLUMN token_blacklist.reason IS '加入黑名单原因';
COMMENT ON COLUMN token_blacklist.expired_at IS 'Token过期时间';
COMMENT ON COLUMN token_blacklist.created_at IS '创建时间';
COMMENT ON COLUMN token_blacklist.updated_at IS '更新时间';

-- Create "app_user" table (renamed from "user" to avoid PostgreSQL reserved word)
CREATE TABLE app_user (
  id bigint NOT NULL,
  username varchar(50) NOT NULL,
  password varchar(100) NOT NULL,
  nickname varchar(50) NOT NULL DEFAULT '',
  email varchar(100) NOT NULL DEFAULT '',
  avatar_file_id bigint NULL,
  default_org_id bigint NOT NULL DEFAULT 0,
  status smallint NOT NULL DEFAULT 1,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_app_user_email ON app_user (email);
CREATE UNIQUE INDEX uk_app_user_username ON app_user (username);
COMMENT ON TABLE app_user IS '系统用户表';
COMMENT ON COLUMN app_user.id IS '用户ID';
COMMENT ON COLUMN app_user.username IS '用户名（登录用）';
COMMENT ON COLUMN app_user.password IS '密码（加密存储）';
COMMENT ON COLUMN app_user.nickname IS '昵称（展示用）';
COMMENT ON COLUMN app_user.email IS '邮箱（登录/通知）';
COMMENT ON COLUMN app_user.avatar_file_id IS '用户头像';
COMMENT ON COLUMN app_user.default_org_id IS '默认组织ID';
COMMENT ON COLUMN app_user.status IS '状态 1-启用 0-禁用';

-- Create "user_operation_log" table
CREATE TABLE user_operation_log (
  id bigint NOT NULL,
  user_id bigint NOT NULL,
  project_id bigint NOT NULL,
  item_id bigint NULL,
  operation_type smallint NOT NULL,
  request_body json NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE user_operation_log IS '用户操作记录表';
COMMENT ON COLUMN user_operation_log.user_id IS '操作人ID';
COMMENT ON COLUMN user_operation_log.project_id IS '所属企划ID';
COMMENT ON COLUMN user_operation_log.item_id IS '所属项目ID';
COMMENT ON COLUMN user_operation_log.operation_type IS '操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)';
COMMENT ON COLUMN user_operation_log.request_body IS '操作请求体';
COMMENT ON COLUMN user_operation_log.created_at IS '操作时间';

-- Create "user_role" table
CREATE TABLE user_role (
  id bigint NOT NULL,
  user_id bigint NOT NULL,
  role_id bigint NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at timestamp NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_user_role ON user_role (user_id, role_id);
COMMENT ON TABLE user_role IS '用户角色关联表';
COMMENT ON COLUMN user_role.id IS '主键ID';
COMMENT ON COLUMN user_role.user_id IS '用户ID';
COMMENT ON COLUMN user_role.role_id IS '角色ID';

-- ============================================================
-- Triggers: 自动更新 updated_at
-- ============================================================
CREATE TRIGGER update_admin_updated_at BEFORE UPDATE ON admin FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_base_task_updated_at BEFORE UPDATE ON base_task FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_email_verify_otp_updated_at BEFORE UPDATE ON email_verify_otp FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_group_application_updated_at BEFORE UPDATE ON group_application FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_item_updated_at BEFORE UPDATE ON item FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_item_task_flow_updated_at BEFORE UPDATE ON item_task_flow FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_item_task_node_updated_at BEFORE UPDATE ON item_task_node FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_organization_updated_at BEFORE UPDATE ON organization FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_organization_create_otp_updated_at BEFORE UPDATE ON organization_create_otp FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_organization_join_otp_updated_at BEFORE UPDATE ON organization_join_otp FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_organization_member_updated_at BEFORE UPDATE ON organization_member FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_permission_updated_at BEFORE UPDATE ON permission FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_project_updated_at BEFORE UPDATE ON project FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_project_type_updated_at BEFORE UPDATE ON project_type FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_project_workshop_updated_at BEFORE UPDATE ON project_workshop FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_resource_updated_at BEFORE UPDATE ON resource FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_role_updated_at BEFORE UPDATE ON role FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_role_permission_updated_at BEFORE UPDATE ON role_permission FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_scheduled_task_log_updated_at BEFORE UPDATE ON scheduled_task_log FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_task_instance_updated_at BEFORE UPDATE ON task_instance FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_task_submission_updated_at BEFORE UPDATE ON task_submission FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_task_template_updated_at BEFORE UPDATE ON task_template FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_task_template_node_updated_at BEFORE UPDATE ON task_template_node FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_token_blacklist_updated_at BEFORE UPDATE ON token_blacklist FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_app_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_operation_log_updated_at BEFORE UPDATE ON user_operation_log FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_role_updated_at BEFORE UPDATE ON user_role FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
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
