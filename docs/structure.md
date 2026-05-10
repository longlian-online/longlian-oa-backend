# 目录结构

## 模块结构

```
longlian-oa-backend/
├── common/          # 公共模块：枚举、注解
├── generator/       # 代码生成器模块
└── app/             # 主应用模块
```

## Controller 层

| 目录 | 说明 |
|------|------|
| `controller/admin/` | 管理端接口（含定时任务手动触发） |
| `controller/app/` | 用户端接口 |
| `controller/orgadmin/` | 组织管理端接口 |
| `controller/common/` | 通用接口（文件上传等） |

## Service 层

| 目录 | 说明 |
|------|------|
| `service/user/` | 用户/会话/项目/工作坊/会话服务 |
| `service/admin/` | 系统管理服务 |
| `service/orgadmin/` | 组织管理服务 |
| `service/notify/` | 通知服务（邮件） |
| `service/resource/` | 文件存储服务 |
| `service/scheduled/` | 定时任务日志服务 |
| `service/common/` | 通用服务 |

## 定时任务

| 路径 | 说明 |
|------|------|
| `scheduled/ScheduledTask.java` | 任务接口，所有定时任务实现此接口 |
| `scheduled/ScheduledTaskEngine.java` | 调度引擎，启动时扫描所有 ScheduledTask Bean 并注册 cron 任务 |
| `scheduled/task/` | 任务实现目录（如 HeartbeatTask） |
| `service/scheduled/` | 任务日志服务 |
| `controller/admin/ScheduledTaskController.java` | 手动触发定时任务 API |
| `pojo/bo/ScheduledTaskDefinition.java` | 任务定义 BO |
| `pojo/entity/ScheduledTaskLog.java` | 任务执行日志实体 |

## 数据库

数据库迁移文件位于 `app/src/main/resources/manifest/migrate/`

命名规范：`v{主版本}-{次版本}-{修订版本}.sql`

主要表结构：
- **RBAC**: permission, role, role_permission, user, user_role
- **Organization**: organization, organization_member, group_application
- **OTP**: one_time_password, email_verify_otp, organization_create_otp, organization_join_otp
- **Project/Work**: project, project_type, item, base_task, task_template, task_template_node, item_task_flow, item_task_node, task_instance, task_submission, user_operation_log, project_workshop
- **Resource**: resource
- **Admin**: admin, token_blacklist
- **Scheduled**: scheduled_task_log