# LongLian-OA 后端优化演进计划

> 创建时间：2026-05-14
> 最后更新：2026-05-14
> 状态：待处理

---

## 目录

- [A. 工程质量](#a-工程质量)
- [B. 可观测性](#b-可观测性)
- [C. 运维能力](#c-运维能力)
- [D. 安全性](#d-安全性)
- [E. 架构演进](#e-架构演进)
- [执行优先级](#执行优先级)

---

## A. 工程质量

### A.1 测试覆盖

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 高 | Service层单元测试严重不足 | 仅有2个简陋测试 | 待处理 |
| 中 | API测试覆盖需扩展 | 13个集成测试 | 待处理 |
| 低 | 清理@Disabled测试 | `AdminBootstrapTest.java` | 待处理 |

**行动项：**
- [ ] 为核心Service添加单元测试（UserServiceImpl, OrganizationMemberServiceImpl等）
- [ ] 补充边界条件测试（空列表、超长字符串、特殊字符）
- [ ] 清理或删除无效的@Disabled测试

### A.2 代码重复

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 中 | BeanUtils.copyProperties滥用 (18处) | 多个Controller | 待处理 |
| 低 | DTO结构高度相似 | 多个ListDTO | 待处理 |

**行动项：**
- [ ] 统一使用Assembler模式替代BeanUtils.copyProperties
- [ ] 抽取公共分页DTO父类PageRequestDTO的扩展

### A.3 代码规范

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | TODO Stub代码 - 8个接口直接返回null | `TaskInstanceController.java` | 待处理 |
| **高** | TODO Stub代码 - 6个接口直接返回null | `ProjectController.java` | 待处理 |
| 中 | 硬编码`LIMIT 1` (10处) | 多个Service | 待处理 |
| 低 | PO/BO包结构混乱 | `pojo/bo/` | 待处理 |

**行动项：**
- [ ] 实现TaskInstanceController所有TODO接口
- [ ] 实现ProjectController所有TODO接口
- [ ] 替换硬编码`LIMIT 1`为MyBatis Plus的`.one()`方法
- [ ] 按模块分包整理pojo/bo/

### A.4 代码异味

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | 空指针风险 - 未检查organization为null | `OrgAdminOrganizationServiceImpl.java:25` | 待处理 |
| 中 | UserServiceImpl过大 (278行) | `service/user/impl/UserServiceImpl.java` | 待处理 |
| 中 | OrganizationMemberController过大 (218行) | `controller/orgadmin/OrganizationMemberController.java` | 待处理 |
| 低 | 空ProjectMapper.xml | `mapper/xml/ProjectMapper.xml` | 待处理 |

**行动项：**
- [ ] 添加organization null检查，抛出AppException
- [ ] 拆分UserServiceImpl过大的方法
- [ ] 拆分OrganizationMemberController或按职责拆分

---

## B. 可观测性

### B.1 日志规范

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | TraceId未写入MDC - 日志无法关联 | `TraceIdFilter.java` | 待处理 |
| 中 | 日志格式缺少userId、ip等关键字段 | `logback-spring.xml` | 待处理 |
| 中 | ERROR日志缺少异常堆栈 | `AuthenticationEntryPointImpl.java:50` | 待处理 |
| 低 | 关键业务操作无结构化日志 | `SessionServiceImpl.java` | 待处理 |

**行动项：**
- [ ] 修复TraceIdFilter，在doFilter中`MDC.put("trace_id", traceId)`，结束时remove
- [ ] 增强logback日志格式，添加userId、ip等字段
- [ ] 确保所有log.error记录异常堆栈
- [ ] 关键业务操作（登录、登出）添加结构化日志

### B.2 监控指标

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | Actuator端点未暴露 | `application.yml` | 待处理 |
| **高** | 无自定义业务指标 | Micrometer未使用 | 待处理 |
| 中 | 无健康检查自定义指标 | 缺失HealthIndicator | 待处理 |
| 低 | JVM指标未暴露 | `application.yml` | 待处理 |

**行动项：**
- [ ] 配置Actuator端点暴露（health,info,metrics,prometheus）
- [ ] 引入micrometer-registry-prometheus
- [ ] 添加自定义业务指标（登录次数、API响应时间等）
- [ ] 添加自定义HealthIndicator（DB、Redis连接池）

### B.3 链路追踪

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | OpenTelemetry仅有API无SDK | `app/pom.xml` | 待处理 |
| **高** | traceId未透传到其他服务 | 无Feign拦截器 | 待处理 |
| 中 | MyBatis未配置OpenTelemetry拦截器 | `application.yml` | 待处理 |

**行动项：**
- [ ] 添加opentelemetry-sdk和exporter-otlp依赖
- [ ] 配置OpenTelemetry自动拦截器（HTTP、JDBC）
- [ ] 添加Feign/RestTemplate的traceId传播拦截器

### B.4 告警机制

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | 告警机制完全缺失 | - | 待处理 |
| **高** | 定时任务失败无通知 | `ScheduledTaskEngine.java:128` | 待处理 |
| 中 | 业务异常无告警 | `GlobalExceptionHandler.java` | 待处理 |

**行动项：**
- [ ] 集成AlertManager或类似告警系统
- [ ] 定时任务失败时调用NotificationManager发送告警
- [ ] 业务错误率飙升时发送告警

---

## C. 运维能力

### C.1 API限流熔丝

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | API限流机制缺失 | 全局 | 待处理 |
| **高** | 服务熔丝机制缺失 | 全局 | 待处理 |

**行动项：**
- [ ] 引入Resilience4j或Sentinel
- [ ] 实现API级别限流
- [ ] 实现服务熔丝，防止级联故障

### C.2 容器化部署

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 中 | 无生产级docker-compose | `devops/` | 待处理 |
| 中 | 无K8s配置 | 项目根目录 | 待处理 |
| 低 | 无Helm Chart | - | 待处理 |

**行动项：**
- [ ] 添加docker-compose.prod.yml
- [ ] 添加k8s部署清单
- [ ] 创建Helm Chart（可选）

### C.3 健康检查

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | Actuator未配置+SecurityConfig未放行 | `application.yml`, `SecurityConstants.java` | 待处理 |
| 中 | 无自定义健康指标 | 缺失HealthIndicator | 待处理 |
| 低 | 无容器探针配置 | docker-compose | 待处理 |

**行动项：**
- [ ] 配置Actuator端点并放行Security
- [ ] 添加DB/Redis连接池健康检查
- [ ] 在docker-compose中添加readinessProbe/livenessProbe

### C.4 配置管理

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 中 | 缺少application-prod.yml | `app/src/main/resources/` | 待处理 |
| 低 | .env包含明文密码 | `.env` | 待处理 |

**行动项：**
- [ ] 添加application-dev.yml和application-prod.yml
- [ ] 使用Docker Secrets或Vault管理敏感信息

---

## D. 安全性

### D.1 权限模型

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | 管理端`/admin/**`无权限校验 | `JwtAuthenticationFilter.java:79-86` | 待处理 |
| 中 | admin接口未使用@PreAuthorize | 多个AdminController | 待处理 |

**行动项：**
- [ ] 管理端接口添加@PreAuthorize权限校验
- [ ] 统一管理端和orgadmin端的权限模型

### D.2 敏感数据保护

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 中 | 异常信息可能泄露敏感数据 | `GlobalExceptionHandler.java:24-31` | 待处理 |
| 低 | 日志没有敏感信息脱敏 | `logback-spring.xml` | 待处理 |
| 低 | 文件上传没有内容类型检测 | `PatternConstants.java:5-6` | 待处理 |

**行动项：**
- [ ] 异常信息脱敏处理
- [ ] 日志添加敏感字段脱敏
- [ ] 文件上传增加内容类型检测

### D.3 审计日志

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 中 | 审计日志实体存在但未实现 | `UserOperationLog.java` | 待处理 |

**行动项：**
- [ ] 实现操作审计AOP拦截器
- [ ] 记录关键业务操作（创建/修改/删除）

### D.4 API安全

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | 登录无失败次数限制/账户锁定 | `AdminSessionServiceImpl.java:51-52` | 待处理 |
| **高** | 验证码无尝试次数限制 | `EmailVerifyService.java:96-108` | 待处理 |
| 中 | 缺少CORS配置 | - | 待处理 |
| 中 | 缺少安全HTTP头 | - | 待处理 |
| 中 | 缺少全局请求体大小限制 | - | 待处理 |

**行动项：**
- [ ] 实现登录失败锁定机制（如5次失败锁定15分钟）
- [ ] 验证码增加错误次数限制（如5次错误则失效）
- [ ] 配置CORS
- [ ] 添加安全HTTP头（X-Frame-Options等）
- [ ] 配置全局请求体大小限制

### D.5 Token安全

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 低 | Token黑名单机制良好 | `TokenBlacklistServiceImpl.java` | 已实现 |

---

## E. 架构演进

### E.1 模块化

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | common模块被低估 | `app/common/` | 待处理 |
| 中 | impl目录位置不规范 | `service/impl/` | 待处理 |

**行动项：**
- [ ] 抽取app/common/下的config/constants/exception/result等到common模块
- [ ] 移动service/impl/到对应服务子目录

### E.2 领域驱动

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 中 | Service层按端划分非按领域 | `service/admin/`, `service/orgadmin/` | 待处理 |
| 低 | UserServiceImpl职责过多 | `service/user/impl/UserServiceImpl.java` | 待处理 |

**行动项：**
- [ ] 考虑按领域重新组织service层（user/organization/project/task）
- [ ] 拆分UserServiceImpl过大的方法

### E.3 技术债务

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| **高** | Redis反序列化安全风险 | `RedisCacheConfig.java:33` | 待处理 |
| **高** | jjwt API过时 (0.12已deprecated) | `JwtUtil.java:37` | 待处理 |
| **高** | ScheduledTaskEngine无并发控制 | `ScheduledTaskEngine.java:111` | 待处理 |
| 中 | 硬编码缓存配置 | `LocalCacheConfig.java:32-34` | 待处理 |
| 中 | 缓存抽象配置了但未使用 | 全局无`@Cacheable` | 待处理 |
| 低 | mysql-connector应替换 | `pom.xml` | 待处理 |

**行动项：**
- [ ] 替换`LaissezFaireSubTypeValidator`为白名单机制
- [ ] 升级jjwt到0.12.x并更新API
- [ ] 添加Redis分布式锁实现定时任务并发控制
- [ ] 缓存配置抽取到配置文件
- [ ] 在适合场景使用缓存注解
- [ ] 替换mysql-connector为mysql-connector-j

### E.4 扩展性

| 优先级 | 问题 | 位置 | 状态 |
|--------|------|------|------|
| 低 | BeanUtils.copyProperties可替换 | 多个Controller | 待处理 |

**行动项：**
- [ ] 考虑使用MapStruct替代BeanUtils

---

## 执行优先级

### 立即修复 (1周内)

| # | 问题 | 涉及方向 |
|---|------|----------|
| 1 | 管理端`/admin/**`权限绕过 | D-安全 |
| 2 | 登录无失败次数限制 | D-安全 |
| 3 | 验证码无尝试次数限制 | D-安全 |
| 4 | Redis反序列化安全风险 | E-架构 |
| 5 | 空指针风险 | A-工程 |
| 6 | TODO Stub代码 | A-工程 |

### 近期实现 (1个月)

| # | 问题 | 涉及方向 |
|---|------|----------|
| 7 | Actuator + 健康检查 | B-可观测性, C-运维 |
| 8 | API限流熔丝 | C-运维 |
| 9 | TraceId MDC修复 | B-可观测性 |
| 10 | common模块代码抽取 | E-架构 |
| 11 | 审计日志实现 | D-安全 |
| 12 | 定时任务并发控制 | E-架构 |

### 持续优化 (3个月+)

| # | 问题 | 涉及方向 |
|---|------|----------|
| 13 | 领域驱动重构Service层 | E-架构 |
| 14 | 单元测试覆盖提升 | A-工程 |
| 15 | K8s容器编排 | C-运维 |
| 16 | 监控告警体系 | B-可观测性 |
| 17 | jjwt升级 | E-架构 |

---

## Changelog

| 日期 | 描述 |
|------|------|
| 2026-05-14 | 初始创建优化演进计划 |
