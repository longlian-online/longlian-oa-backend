# LongLian-OA Backend

<div align="center">
  <img align="center" src="https://longlian-oa-sit-1308050490.cos.ap-guangzhou.myqcloud.com/logo.jpg" height="96" width="96" alt="logo"/>
  <h1 align="center">
   LongLian-OA
  </h1>
</div>

[![DeepSource](https://app.deepsource.com/gh/longlian-online/longlian-oa-backend.svg/?label=code+coverage&show_trend=false&token=y6MhTlTi6rIPFrHHYU3JlkWj)](https://app.deepsource.com/gh/longlian-online/longlian-oa-backend/)

---

## 项目架构

```
longlian-oa-backend/
├── common/                      # 公共模块（枚举、注解、工具类）
│   └── src/main/java/online/longlian/common/
│       ├── enumeration/         # 枚举类（CodeEnum, EmailVerifyBusinessType 等）
│       └── annotation/          # 注解（@ModelEnum, @ModelEnums）
├── generator/                   # 代码生成器模块
│   └── src/main/java/online/longlian/generator/
├── app/                         # 主应用模块
│   └── src/main/java/online/longlian/app/
│       ├── controller/          # 控制器层
│       │   ├── admin/           # 管理端接口
│       │   ├── app/             # 用户端接口
│       │   ├── orgadmin/        # 组织管理端接口
│       │   └── common/          # 通用接口
│       ├── service/             # 业务逻辑层
│       │   ├── user/            # 用户/会话/项目服务
│       │   ├── admin/           # 系统管理服务
│       │   ├── orgadmin/        # 组织管理服务
│       │   ├── notify/          # 通知服务（邮件）
│       │   ├── resource/        # 文件存储服务
│       │   ├── scheduled/       # 定时任务日志服务
│       │   └── common/          # 通用服务
│       ├── scheduled/           # 定时任务调度核心
│       │   ├── ScheduledTask.java        # 任务接口
│       │   ├── ScheduledTaskEngine.java   # 调度引擎
│       │   └── task/                     # 任务实现
│       ├── mapper/              # MyBatis Mapper 接口
│       ├── mapper/xml/          # MyBatis Mapper XML
│       ├── pojo/                 # 数据对象层
│       │   ├── entity/          # 数据库实体
│       │   ├── dto/             # 请求体 DTO
│       │   ├── vo/              # 响应体 VO
│       │   └── bo/              # 业务对象
│       └── common/              # 通用核心代码
│           ├── config/          # Spring 配置类
│           ├── constants/       # 常量定义
│           ├── enumeration/     # 枚举类
│           ├── exception/       # 自定义异常
│           ├── filter/           # 过滤器（JWT 鉴权、链路追踪）
│           ├── handler/          # 处理器
│           ├── properties/       # 配置属性类
│           ├── result/          # 统一响应封装
│           ├── security/        # 安全相关
│           └── util/            # 工具类
├── docs/                        # 项目文档
├── devops/                      # DevOps 配置
├── pom.xml                      # 父 POM（依赖管理）
└── AGENTS.md                    # Agent 指令
```

---

## 技术栈

| 分类 | 技术 |
|------|------|
| 框架 | Spring Boot 3.3.5 |
| Java | 21 |
| ORM | MyBatis Plus 3.5.15 |
| 认证 | Spring Security + JWT (jjwt 0.11.5) |
| 缓存 | Redis + Caffeine 本地缓存 |
| 连接池 | Druid |
| 文件存储 | 腾讯云 COS + 本地存储 |
| 邮件 | Spring Mail（异步发送）|
| API 文档 | Springdoc OpenAPI (Swagger) |
| 链路追踪 | OpenTelemetry |
| 序列化 | Fastjson2 |

---

## 关键基础设施

| 能力 | 实现方式 |
|------|----------|
| 异步执行 | `@Async` + 虚拟线程（`VirtualThreadTaskExecutor`）|
| 认证鉴权 | `JwtAuthenticationFilter` 从 Header 解析 JWT |
| JWT 黑名单 | 登出时加入 Redis 黑名单（`RedisBlacklistUtil`）|
| 文件上传 | `ResourceService` 统一入口，`StorageServiceFactory` 自动选择 |
| 通知 | `NotificationManager` + `EmailNotificationService` |
| 链路追踪 | `TraceIdFilter` 生成 TraceId |
| 定时任务 | `ScheduledTask` 接口 + `ScheduledTaskEngine` 调度引擎 |

---

## 数据库

数据库迁移文件位于 `app/src/main/resources/manifest/migrate/`

命名规范：`v{主版本}-{次版本}-{修订版本}.sql`