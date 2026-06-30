# AGENTS.md

## 项目概述

- **类型**: Spring Boot 3.3.5 REST API 后端
- **Java**: 21
- **数据库**: MySQL + MyBatis Plus 3.5.15 ORM
- **认证**: Spring Security + JWT (jjwt 0.11.5)
- **构建**: Maven 多模块 (app, generator)
- **缓存**: Redis 缓存
- **连接池**: Druid
- **文件存储**: 腾讯云 COS (OSS) + 本地存储
- **邮件**: Spring Mail
- **API 文档**: Springdoc OpenAPI (Swagger)
- **链路追踪**: OpenTelemetry
- **序列化**: Fastjson2

## 包结构

```
app/src/main/java/online/longlian/app/
├── controller/
│   ├── admin/           # 管理端接口
│   ├── app/             # 用户端接口
│   ├── orgadmin/        # 组织管理端接口
│   └── common/          # 通用接口（文件上传等）
├── service/
│   ├── user/            # 用户/组织/项目/工作坊/会话服务
│   ├── admin/           # 系统管理服务
│   ├── notify/          # 通知服务（邮件等）
│   ├── resource/        # 文件存储服务
│   ├── scheduled/       # 定时任务服务
│   └── common/          # 通用服务
├── scheduled/           # 定时任务调度核心（接口+引擎+任务实现）
│   ├── ScheduledTask.java      # 任务接口
│   ├── ScheduledTaskEngine.java # 调度引擎（实现 ApplicationRunner）
│   └── task/                   # 任务实现（如 HeartbeatTask）
├── mapper/              # MyBatis Mapper 接口 + XML
├── pojo/
│   ├── entity/          # 数据库实体（与表一一对应）
│   ├── dto/             # 请求体 DTO（按 admin/app/orgadmin 分包）
│   ├── vo/              # 响应体 VO（按 admin/app/orgadmin 分包）
│   └── bo/              # 业务对象（多入参时用于 service 层传参）
└── common/
    ├── config/          # Spring 配置类
    ├── constants/       # 常量定义
    ├── enumeration/     # 枚举类
    ├── exception/       # 自定义异常 AppException
    ├── filter/          # JwtAuthenticationFilter（核心鉴权）, TraceIdFilter
    ├── handler/         # GlobalExceptionHandler, 认证/授权失败处理器
    ├── properties/      # @ConfigurationProperties 配置类
    ├── result/          # Result<T>（通用返回体）, ResultCode
    ├── security/        # 认证 Provider, UserDetailsService 实现, Token 类
    └── util/            # 工具类（JWT, 邮件, Redis, 随机码等）
```

## 分层架构约定

```
Controller → Service(interface) → ServiceImpl → Mapper(interface) → XML
     ↓            ↓
   DTO/VO        BO
```

- Controller 只做参数提取和调用 Service，不做业务逻辑
- Service 接口与实现分离，impl 子包存放实现
- Service 层使用 MyBatis Plus 的 `ServiceImpl<Mapper, Entity>` 基类
- Mapper 接口继承 MyBatis Plus 的 `BaseMapper<Entity>`，XML 文件同名放在 `resources/mapper/` 下

## 数据库变更规范

1. 所有表结构变更通过 `app/src/main/resources/manifest/migrate/` 下的 SQL 迁移文件执行，命名格式 `v{主版本}-{次版本}-{修订版本}.sql`
2. **禁止直接修改 entity 类**，变更流程：编写新版本迁移 SQL → 执行迁移 → 使用 `generator` 模块重新生成 entity/mapper/XML
3. generator 模块通过注解驱动代码生成（如 `@ModelEnum` / `@ModelEnums` 生成枚举类）

## 代码规范

1. 接口保持 RESTFul 风格：资源名用复数，路由体现层级关系，如 `POST /app/session/` 而非 `POST /app/user/login`
2. 列表请求复用 `PageRequestDTO`，列表响应复用 `PageResultVO`
3. 请求体必须用 DTO 接收，响应体必须用 VO 返回，禁止直接暴露 entity
4. 完全信任 Controller 预处理后的入参，Service 层不做重复非空判断、不做分页参数兜底、不做防御性编码
5. 多入参时定义独立入参类（如 service 层使用 BO 类），以便后续扩展
6. 注释应说明「为什么这样做」而非「做了什么」（逻辑简单时无需注释）

## 通用返回格式

```java
Result<T>  // code=0 成功, 非0 异常; msg 提示; data 业务数据
```
异常统一通过 `AppException` 抛出，由 `GlobalExceptionHandler` 捕获转换为 `Result`。

## 已有基础设施（可直接复用）

| 能力 | 关键类/方式 |
|---|---|
| 异步执行 | `@Async` + 虚拟线程（`VirtualThreadTaskExecutor`），如邮件发送 |
| 认证鉴权 | `JwtAuthenticationFilter` 从 Header 解析 JWT；支持邮箱验证码 + 用户名密码两种登录 |
| JWT 黑名单 | 登出时 JWT 加入 Redis 黑名单 (`TokenBlacklistService`) |
| 文件上传 | `ResourceService` 统一入口，`StorageServiceFactory` 根据配置自动选 OSS/本地 |
| 通知 | `NotificationManager` + `EmailNotificationService`，异步发送邮件 |
| 链路追踪 | `TraceIdFilter` 生成 TraceId，OpenTelemetry 自动埋点 |
| 定时任务 | `ScheduledTask` 接口定义任务，`ScheduledTaskEngine` 调度引擎（Spring Cron），手动触发通过 `admin/ScheduledTaskController` |

## 项目中不具备的基础设施（如需使用需从零搭建）

- 分布式锁（Redis 可用但无封装）
- 消息队列

## Pull Request

生成 pull request 消息必须参考 docs/pull_request_template.md 的格式

## 测试

### API 测试

编写 API 测试需要遵循 @docs/api_test_standard.md 文档