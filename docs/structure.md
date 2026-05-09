# 目录结构

## Controller

1. admin: 管理端接口（含定时任务手动触发接口）
2. app: 用户端接口
3. orgadmin: 组织管理端接口

目录下放置共享接口（文件上传等）

## 定时任务

| 路径 | 说明 |
|---|---|
| `app/src/main/java/online/longlian/app/scheduled/ScheduledTask.java` | 任务接口，所有定时任务实现此接口 |
| `app/src/main/java/online/longlian/app/scheduled/ScheduledTaskEngine.java` | 调度引擎，启动时扫描所有 ScheduledTask Bean 并注册 cron 任务 |
| `app/src/main/java/online/longlian/app/scheduled/task/` | 任务实现目录（如 HeartbeatTask） |
| `app/src/main/java/online/longlian/app/service/scheduled/` | 任务日志服务 |
| `app/src/main/java/online/longlian/app/controller/admin/ScheduledTaskController.java` | 手动触发定时任务 API |
| `app/src/main/java/online/longlian/app/pojo/bo/ScheduledTaskDefinition.java` | 任务定义 BO |
| `app/src/main/java/online/longlian/app/pojo/entity/ScheduledTaskLog.java` | 任务执行日志实体 |