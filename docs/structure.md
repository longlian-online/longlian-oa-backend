# 目录结构

## 模块结构

```
longlian-oa-backend/
├── common/          # 公共模块：枚举、注解与分布式锁基础能力
├── generator/       # 代码生成器模块
└── app/             # 主应用模块
```

## app 模块总览

`app/` 是可运行的 Spring Boot 主应用模块。以下路径均相对后端仓库根目录；Java 包内路径相对 `app/src/main/java/online/longlian/app/`。

```text
app/                                   # 主应用的构建、生产源码与测试
├── pom.xml                            # 应用依赖、打包与测试构建配置
└── src/                               # 源码与资源
    ├── main/                          # 随应用构建的生产内容
    │   ├── java/                      # Java 源码根目录
    │   │   └── online/longlian/app/    # 应用根包，online/longlian 为命名空间
    │   │       ├── AppApplication.java # Spring Boot 应用启动入口
    │   │       ├── common/            # 应用公共基础设施
    │   │       ├── controller/        # 按端别组织的 HTTP 接口
    │   │       ├── service/           # 按端别、业务域组织的业务服务
    │   │       ├── mapper/            # 数据访问接口
    │   │       ├── pojo/              # 实体、请求、响应和业务对象
    │   │       ├── mcp/               # MCP 工具入口
    │   │       └── scheduled/         # 定时任务调度核心与任务入口
    │   └── resources/                 # 运行配置、SQL 映射与邮件模板
    └── test/                          # 测试内容，不随生产应用发布
        ├── java/online/longlian/app/   # 测试根包，按测试对象与类型分包
        └── resources/                 # 测试配置与 Spring 测试扩展注册
```

`app/common/` Java 包属于主应用内部基础设施，与仓库根目录的 `common/` Maven 公共模块不同。Controller、Service、Mapper 是处理请求的分层；POJO 承载各层数据，公共基础设施为各层提供运行支持。

典型请求经过认证过滤器、会话参数解析与组织范围校验后，由 Controller 接收 DTO、调用 Service，Service 使用 BO 表达业务输入与结果并通过 Mapper 访问 Entity；Controller 将结果转换为 VO，由响应处理器统一包装。具体规范见 [项目规范](standard.md)。

## Controller 层

`controller/` 是 HTTP 入口层，负责路由、参数绑定与校验、权限声明、DTO/BO/VO 转换和服务调用；业务规则放在 Service。各端目录直接存放控制器，目前没有进一步的业务子包。

| 目录 | 说明 |
|------|------|
| `controller/admin/` | 管理端接口（含定时任务手动触发） |
| `controller/app/` | 用户端接口 |
| `controller/orgadmin/` | 组织管理端接口 |
| `controller/common/` | 通用接口（文件上传等） |

## Service 层

业务目录在前。有接口与实现分离的服务，接口放在域根，实现在域内 `impl/`；直接提供能力的具体类可以位于域根：

```text
service/
├── app/<业务域>/          # 用户端：session、user、project、projectworkshop、
│                          # item、itemtaskflow、taskinstance、mytask、
│                          # taskplaza、tasktemplate、notification
├── app/taskstage/         # 任务阶段共享规则（SlotLabels、StageUnlockPolicy）
├── orgadmin/<业务域>/     # 组织管理端：organization、member、basetask、presetfield、
│                          # presettag、project、projecttype、tasktemplate、file
├── admin/<业务域>/        # 平台管理端：account、session、organization、storage
├── auth/                  # 令牌黑名单与刷新会话存储
├── organization/          # 跨端组织成员能力与有效提交统计
├── audit/                 # 用户操作审计
├── taskform/              # 任务表单契约与校验
├── system/                # 系统信息
├── lock/                  # 分布式锁封装
├── deletion/              # 删除申请（用户端发起、组织管理端审核）
├── resource/              # 文件存储与资源生命周期
├── notify/                # 通知发送
├── otp/                   # 验证码与邀请策略
├── outbox/                # 事务外异步投递
├── productrelease/        # 产品版本发布
└── scheduled/             # 定时任务日志
```

- 有接口与实现分离的业务域，其接口放在域根、实现在 `impl/`；实现内部的 Assembler、Handler、QueryBuilder 等辅助类随实现存放。跨域调用的具体服务放在域根，避免依赖其他域的 `impl/`。
- Service 业务域不要求与 Controller 的端别一一对应：`admin` 承载平台管理业务，顶层共享域由各端接口或其他服务调用。同时被多个端别调用的服务（如 `deletion` 用户端发起 + 组织管理端审核、`productrelease` 平台管理 + 用户查看）放在顶层业务域，不放任一端别包下。
- 只有需要多实现或跨模块替换的组件才定义接口；单一实现的内部支撑类可以保持具体类，不为形式补接口。OTP 策略统一实现 `OTPStrategyService`。
- `taskstage` 只放被 taskinstance、mytask、taskplaza、notification 共用的阶段计算规则，不承载业务状态，该域不设 `impl/`。

### service/app 业务域

用户端业务服务按业务域组织，接口在域根、实现在 `impl/`。共 11 个业务域 + 1 个共享规则域：

| 域 | 职责 | 关键类型 |
|------|------|----------|
| `session/` | 登录、登出、刷新会话 | `SessionService` / `SessionServiceImpl` |
| `user/` | 用户资料、配置、密码、注册与入组 | `UserService` / `UserServiceImpl`、`NotificationKindPreference` |
| `project/` | 企划、企划附件、成员与搜索 | `ProjectService`、`ProjectAttachmentService` / `ProjectServiceImpl`、`ProjectAttachmentServiceImpl`、`ProjectAssembler`、`ProjectQueryBuilder`、`ProjectSearchSupport`、`ProjectProgressHandler`、`ProjectAttachmentHandler`、`ProjectMetadataValidator`、`ProjectCustomFieldRequirements` |
| `projectworkshop/` | 个人工坊列表、企划加入与退出 | `ProjectWorkshopService` / `ProjectWorkshopServiceImpl`、`WorkshopAssembler`、`WorkshopProjectHandler`、`WorkshopQueryBuilder` |
| `item/` | 项目创建、列表、排序、删除 | `ItemService` / `ItemServiceImpl`、`ItemAssembler`、`ItemQueryBuilder` |
| `itemtaskflow/` | 项目任务流查询与编辑 | `ItemTaskFlowService` / `ItemTaskFlowServiceImpl`、`ItemTaskFlowAssembler`、`ItemTaskFlowEditor`、`ItemTaskFlowEditPlanner` |
| `taskinstance/` | 任务接取、提交、打回、重置、详情 | `TaskInstanceService` / `TaskInstanceServiceImpl`、`TaskInstanceAssembler`、`TaskInstanceCommandHandler`、`TaskAttachmentPresenter` |
| `mytask/` | 我的待办与组员已接取任务负载 | `MyTaskService`、`ClaimedTaskLoadService` / `MyTaskServiceImpl`、`ClaimedTaskLoadServiceImpl` |
| `taskplaza/` | 任务广场（缺人任务）查询与分组 | `TaskPlazaService` / `TaskPlazaServiceImpl`、`TaskPlazaAssembler` |
| `tasktemplate/` | 创建项目可用的流程模板选项 | `TaskTemplateService` / `TaskTemplateServiceImpl` |
| `notification/` | 站内通知、邮件载荷与 outbox 处理 | `InboxNotificationService` / `InboxNotificationServiceImpl`、`NotificationSourceResolver`、`AdminReviewNotificationPublisher`、`InboxEmailOutboxHandler`、`InboxEmailOutboxWriter`、`InboxEmailLink`、`InboxEmailCopy`、`InboxEmailPayload` |
| `taskstage/` | 任务阶段前置判定与槽位计算（跨域共享规则） | `SlotLabels`、`StageUnlockPolicy` |

- `notification/impl` 中的 `InboxEmailLink`、`InboxEmailCopy` 是邮件文案的纯静态工具，`InboxEmailPayload` 是 outbox 投递的内部载荷，均随实现存放，不搬入 `pojo/bo`。
- `notification/AdminReviewNotificationPublisher` 是供入组与删除申请流程调用的审核通知发布能力，位于域根。
- `taskstage` 被 `taskinstance`、`mytask`、`taskplaza`、`notification` 四个域引用，是纯静态规则类，因此不设接口与 `impl/`。

### service/admin 与 service/orgadmin 业务域

`service/admin/` 承载平台管理服务，`service/orgadmin/` 承载组织管理服务。两者在各业务域根目录定义服务接口，在该域的 `impl/` 存放实现及辅助类；可直接复用的具体服务也位于域根。

| 目录 | 职责 |
|------|------|
| `admin/account/` | 平台管理员账号维护 |
| `admin/session/` | 平台管理员登录与会话 |
| `admin/organization/` | 平台侧组织管理 |
| `admin/storage/` | 平台侧存储用量统计 |
| `orgadmin/organization/` | 当前组织信息读取与维护 |
| `orgadmin/member/` | 组织成员、入组审核、邀请码与提交统计 |
| `orgadmin/basetask/` | 原子任务维护与查询 |
| `orgadmin/project/` | 组织侧企划管理 |
| `orgadmin/projecttype/` | 企划类型与类型自定义表单 |
| `orgadmin/tasktemplate/` | 组织流程模板及节点维护 |
| `orgadmin/presetfield/` | 组织预设字段维护 |
| `orgadmin/presettag/` | 组织预设标签维护 |
| `orgadmin/file/` | 组织文件查询、下载与废弃 |

### Service 内部层次与共享域

`service/` 是业务层；其下的 `app/`、`admin/`、`orgadmin/` 表达使用场景，各业务域目录表达职责边界，`impl/` 表达实现细节。顶层共享域与端别目录并列，供各端业务复用。域的具体职责见本节目录树。

- 域根目录存放对外使用的服务契约；`impl/` 中的 ServiceImpl 编排业务操作，Assembler 组装数据，QueryBuilder 构建查询，Handler 等辅助类分担域内处理。这些辅助类按当前业务需要设置，不要求每个域都具备。
- `auth`、`deletion`、`notify`、`organization`、`otp`、`outbox`、`productrelease`、`resource`、`scheduled`、`system` 均有 `impl/`。其中 `otp/impl/` 存放验证码及邀请策略实现；`resource/impl/` 存放存储策略实现，`ResourceService`、资源清理与签名等具体服务位于 `resource/` 根目录。
- `organization/MemberSubmissionStatisticsService` 统一维护组织成员有效提交次数和最近提交时间，任务域负责调用，不直接维护成员统计。
- `orgadmin/projecttype/ProjectTypePresetFieldService` 负责删除预设字段时清理类型表单引用，不依赖预设字段服务；表单读取与编辑仍由 `ProjectTypeCustomFormService` 负责。
- `audit/`、`lock/`、`taskform/` 当前直接存放具体服务类，没有 `impl/`；`app/taskstage/` 存放纯规则类。目录说明反映现有结构，不为这些类额外增加接口或实现包。
- 包职责以本文为目录索引；存在 `package-info.java` 时，它仅承担包文档职责。

## POJO 层

根包为 `app/src/main/java/online/longlian/app/pojo/`，按对象职责分为以下目录。DTO、VO 和大部分 BO 按端别分包；`bo/app` 和 `bo/orgadmin` 进一步按业务分包，DTO、VO 及 `bo/admin` 仍直接存放对象。

```text
pojo/
├── entity/                 # 数据库实体，按表生成
├── dto/                    # HTTP 请求对象
│   ├── admin/              # 平台管理端请求
│   ├── app/                # 用户端请求
│   ├── orgadmin/           # 组织管理端请求
│   └── common/             # 分页、状态、文件等公共请求对象
├── vo/                     # HTTP 响应对象
│   ├── admin/              # 平台管理端响应
│   ├── app/                # 用户端响应
│   ├── orgadmin/           # 组织管理端响应
│   └── common/             # 分页、文件资源、系统信息、邀请码及跨端
│                           # 共用的企划类型自定义字段响应
└── bo/                     # Service 入参、结果和业务数据结构
    ├── admin/              # 平台管理业务对象
    ├── app/                # 用户端业务对象
    │   ├── user/           # 用户资料、配置及用户所属组织信息
    │   ├── session/        # 登录、登出和刷新会话
    │   ├── project/        # 企划、企划附件、成员和进度
    │   ├── projectworkshop/ # 工坊列表及企划加入、退出工坊
    │   ├── item/           # 项目创建、列表和排序
    │   ├── itemtaskflow/   # 项目任务流编辑
    │   ├── taskinstance/   # 任务实例操作、阶段状态和提交记录
    │   ├── taskplaza/      # 任务广场查询及分组结果
    │   ├── mytask/         # 本人待办及成员已接取任务负载
    │   ├── tasktemplate/   # 任务模板选项及工坊模板维护
    │   └── notification/   # 通知创建、来源解析和已读状态
    ├── orgadmin/           # 组织管理业务对象
    │   ├── organization/   # 组织信息读取和编辑
    │   ├── member/         # 成员、入组申请、邀请码和提交统计
    │   ├── basetask/       # 原子任务及广场任务类型选项
    │   ├── project/        # 企划管理
    │   ├── projecttype/    # 企划类型及自定义字段
    │   ├── tasktemplate/   # 组织任务模板
    │   ├── presetfield/    # 组织预设字段
    │   ├── presettag/      # 组织预设标签
    │   └── file/           # 组织文件列表、展开、下载和废弃
    ├── productrelease/     # 跨端产品发布业务对象
    ├── deletion/           # 跨端删除申请（用户端发起、组织管理端审核）
    └── common/             # 跨端共享对象，含分页、组织成员、资源、
                            # 会话、验证码、调度定义及任务表单
```

### 对象职责与命名

| 类型 | 用途 | 命名示例 |
|------|------|----------|
| Entity | 数据库表映射，不直接作为 HTTP 响应返回 | `Project`、`TaskInstance` |
| DTO | Controller 接收的请求参数或请求体 | `ProjectCreateDTO`、`PageRequestDTO` |
| VO | Controller 返回的展示数据 | `ProjectDetailInfoVO`、`TaskDraftVO` |
| 入参 BO | Service 调用参数 | `ProjectCreateParamsBO`、`TaskFlowNodeDeleteParamsBO` |
| 结果 BO | Service 返回的业务结果 | `DefaultOrganizationResultBO`、`TaskFormValuesResultBO` |
| 业务结构 BO | 业务字段、状态或定义等组成对象 | `TaskFormFieldBO`、`ScheduledTaskDefinitionBO` |

- 名称应包含足够的业务含义，再使用 `DTO`、`VO` 或 `BO` 后缀；Service 入参和结果分别使用 `ParamsBO`、`ResultBO`。不将组成对象机械命名为入参或结果。
- Service 的方法签名只使用 `ParamsBO`、`ResultBO` 和本层业务结构 BO，不使用 VO；HTTP 端点返回 VO 时，由 Controller 把结果 BO 转换为 VO。分页在 Service 使用 `PageResultBO`，在 Controller 使用 `PageResultVO`。
- Mapper 的查询结果只返回实体或 BO，不直接产出端别 VO。
- VO 按提供该接口的端别存放；被多个端别使用的响应放入 `vo/common`，不在端别包中保留同名副本。

### 注解约定

- 可写对象统一使用 `@Data @NoArgsConstructor @AllArgsConstructor @Builder`；VO 额外声明类级 `@Schema`，DTO 使用 `@Data @Schema`。不可变对象可以使用 `@Value @Builder` 或 `@Getter` 加 final 字段，但仍应与同包对象保持同一风格。
- 不因历史写法差异批量补注解；新增或改动对象时按上述口径，避免在同一包内新增第三种签名。
- VO 中需要按字符串输出的 Long 型 ID 使用 `@JsonLongIdString`（仅影响序列化）。请求 DTO 不标该注解，Fastjson2 和 Spring 都能把字符串或数字绑定为 Long。
- VO 的类级与字段级 `@Schema` 同时提供；DTO 的类级 `@Schema` 和每个请求字段的说明同样补齐。BO 不带 `@Schema`，不进入接口文档。
- 需要显式输出 null 的响应（如通知来源、当前产品版本）才使用 `@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)`，该设置只用于前端依赖固定键集合的契约，不作为通用默认。
- 使用能区分用途的最短业务名称，不重复堆叠端别、父对象和动作。例如 `TaskFlowNodeDeleteParamsBO` 表示删除任务流节点所需参数，避免 `RemovedParamsBO` 这类无法独立说明业务的名称。
- 文件名与顶层类型名一致，包声明与文件目录一致。端别由所属包表达；只有区分业务确有需要时才加入类名。
- `class` 和 `record` 都是对象的实现形式，按用途选择并沿用所在业务的风格；不建立 `record` 包，也不因使用 `record` 而省略职责后缀。
- Entity 由 `generator` 根据数据库生成，保留表对应的实体名称，不增加 `BO` 或 `Entity` 后缀，不直接修改生成实体。数据库变更流程见 [项目规范](standard.md)。

### 嵌套类型与对象归属

- 仅用于某个入参或响应的组成类型，可以保留为嵌套类型，但仍需具有明确的业务名称和对应后缀。例如 `ItemTaskFlowUpdateParamsBO.TaskFlowNodeParamsBO`、`TaskInstanceDetailVO.TaskAssigneeVO`。
- 多态响应的接口及实现采用相同职责后缀，例如 `TaskDisplayFieldVO`、`TaskTextFieldVO`、`TaskFileFieldVO`；相关类型引用和 OpenAPI Schema 声明需要一起维护。
- 跨 Service 边界传递的业务数据对象放入 `pojo/bo/`。例如组织成员服务的 `DefaultOrganizationResultBO`、`OrganizationMemberProfileResultBO` 和 `OrganizationMemberDetailsResultBO` 位于 `bo/common/`。
- 实现内部的缓存记录、投递载荷、私有计算结构可以跟随实现存放，不为了统一后缀全部搬入 POJO。对象是否属于 BO 由业务用途决定。
- 优先放入实际所属端别或业务包；`common` 用于跨端共享契约，不作为无法确定归属时的默认目录。新增业务子包时，须同步维护使用方引用和本文目录说明。
- `bo/app` 和 `bo/orgadmin` 的对象放在对应业务子包，不继续向端别根包平铺文件。跨业务引用使用明确的类型导入；拆包时同时检查原先无需导入的同包类型和通配符导入。
- 重命名应同步 Java 引用、测试、Mapper XML 中的完整类名及相关配置。类型改名不应顺带改变字段名、JSON 结构、校验规则或业务行为；调整后编译生产与测试源码，并运行受影响的现有测试。

更完整的分层与数据库规则见 [项目规范](standard.md)。

## Mapper 层

Mapper 接口位于 `app/src/main/java/online/longlian/app/mapper/`，XML 只放在 `app/src/main/resources/mapper/`，文件名与接口同名。每个 Mapper 对应一个实体，统一使用 `@Mapper` 和 `extends BaseMapper<Entity>`。

- 常规筛选、排序、分页和计数在 Service 中通过 `LambdaQueryWrapper` 调用 `BaseMapper`，不写 SQL 注解或 XML。
- 仅当 Wrapper 无法表达时才使用注解或 XML，例如多表关联、窗口函数、JSON 函数、批量 CASE 更新、upsert 或需要包含软删记录。多 statement 或较长 SQL 放 XML，单条较短 SQL 可用注解；同一方法只保留一种实现。
- 使用注解或 XML 时，在方法注释或 SQL 旁用中文说明不能使用 Wrapper 的原因；参数统一使用 `@Param` 和 `#{}` 绑定，禁止 `${}` 拼接外部输入。
- XML 语句 id 与接口方法一一对应；生成器按表产出的空 XML 存根保留，仅删除接口方法时同步删除对应语句。
- Mapper 只读写本实体对应的表。需要其他领域的数据时由所属 Service 提供，不在调用方 Mapper 中拼接对方表。
- 分页优先使用 MyBatis Plus 的 `Page` 参数；聚合查询无法套用分页插件时，手工分页统一写成 `LIMIT #{size} OFFSET #{offset}`。

## 公共基础设施

`common/` 按技术职责分包，为接口、业务服务和数据访问提供统一支持；不在这里集中存放业务域实现。当前各子包直接存放类，没有更深的子目录。

| 目录 | 职责与现有示例 |
|------|----------------|
| `common/annotation/` | 会话注入、响应包装及序列化标记，如 `UserSession`、`NotWrap`、`JsonLongIdString` |
| `common/config/` | Spring 配置与组件注册，包括安全、MVC、数据源、MyBatis、Redis、异步执行及调度 |
| `common/constants/` | 安全、Redis、正则及邀请码等常量 |
| `common/enumeration/` | 应用侧角色、排序、任务视图与通知类型等枚举 |
| `common/exception/` | 统一业务异常 `AppException` |
| `common/filter/` | 请求过滤链中的 JWT 认证与 TraceId 处理 |
| `common/handler/` | 异常与认证失败处理、响应包装，以及枚举数据库类型转换 |
| `common/interceptor/` | MVC 组织范围校验拦截器 `OrganizationScopeInterceptor` |
| `common/mybatis/` | MyBatis 分页扩展 `OffsetPage` |
| `common/properties/` | 应用、存储与跨域等类型化配置属性 |
| `common/resolver/` | 控制器会话参数解析及会话上下文定义 |
| `common/result/` | 统一返回体 `Result` 与结果码 `ResultCode` |
| `common/security/` | 用户与管理员认证策略、认证 Provider、Token 类型、用户详情与当前用户上下文 |
| `common/util/` | JWT、邮件、随机码、TraceId 和下载文件名等工具 |

## MCP 入口

`mcp/` 直接存放 MCP 工具类，目前只有 `PingTools`，通过 `@McpTool` 暴露 `ping` 连通性检查，返回 `pong`。本目录是工具协议入口；当前检查不读写业务数据。

## 定时任务

`scheduled/` 存放调度契约和引擎，`scheduled/task/` 存放具体任务入口；业务能力由对应 Service 提供。它与 `service/scheduled/` 的日志服务职责不同。

| 路径 | 说明 |
|------|------|
| `scheduled/ScheduledTask.java` | 任务接口，所有定时任务实现此接口 |
| `scheduled/ScheduledTaskEngine.java` | 调度引擎，启动时扫描所有 ScheduledTask Bean 并注册 cron 任务 |
| `scheduled/task/` | 任务实现目录（如 HeartbeatTask） |
| `service/scheduled/` | 任务日志服务 |
| `controller/admin/ScheduledTaskController.java` | 手动触发定时任务 API |
| `pojo/bo/common/ScheduledTaskDefinitionBO.java` | 任务定义 BO |
| `pojo/entity/ScheduledTaskLog.java` | 任务执行日志实体 |

## 运行资源

`app/src/main/resources/` 是应用运行时的类路径资源根目录。

| 路径 | 职责 |
|------|------|
| `application.properties` | 应用基础属性配置 |
| `application.yml.example` | 部署配置示例 |
| `application-dev.yml`、`application-local.yml` | 对应环境的配置文件 |
| `logback-spring.xml` | 日志输出配置 |
| `mapper/` | MyBatis XML 映射，文件与 Java Mapper 接口同名，当前直接平铺存放 |
| `templates/` | 邮件 HTML 模板，当前包含验证码与站内通知邮件模板 |

## 测试目录

`app/src/test/java/online/longlian/app/` 是测试根包。`online/longlian/app` 与生产包共享命名空间；下面按测试对象或测试类型组织。

| 目录 | 职责与下层组织 |
|------|----------------|
| `api/` | HTTP API 集成测试；`admin/`、`app/`、`orgadmin/`、`common/` 按端别组织，`mcp/`、`notification/`、`session/` 验证对应协议或场景，`util/` 提供测试辅助设施 |
| `controller/` | 控制器层测试，下分 `app/`、`orgadmin/`、`common/` |
| `service/` | 业务服务测试；按端别、业务域及存在的 `impl/` 对应生产服务组织，当前只为有测试的域建目录 |
| `common/` | 公共基础设施测试，下分 `annotation/`、`config/`、`constants/`、`filter/`、`handler/`、`interceptor/`、`security/`、`util/`、`validation/`；测试包不要求与生产目录完全对称 |
| `pojo/` | 数据对象测试；`bo/common/` 验证共享业务对象，`dto/app/` 验证用户端请求对象 |
| `mcp/` | MCP 工具类测试 |
| `scheduled/task/` | 具体定时任务测试 |

`app/src/test/resources/` 存放 `application-test.yml` 测试环境配置；其下 `META-INF/spring.factories` 注册 Spring 测试执行监听器。单元测试与 API 测试的编写规则分别见 [单元测试规范](unit_test.md) 和 [API 测试规范](api_test_standard.md)。

## 数据库

数据库使用 Atlas 声明式管理，文件位于根目录 `db/`

- 期望状态：`db/schema.sql`（唯一真实来源）
- CI 脚本：`db/migrate.sh`
- 开发文档：`db/DEVELOPMENT.md`

主要表结构：
- **用户与授权**: user；当前授权使用 organization_member.org_role 与 admin.role。permission、role、role_permission、user_role 属于冻结的历史 RBAC 模型，不参与当前鉴权
- **Organization**: organization, organization_member, group_application
- **OTP**: one_time_password, email_verify_otp, organization_create_otp, organization_join_otp
- **Project/Work**: project, project_type, item, base_task, task_template, task_template_node, item_task_flow, item_task_node, task_instance, task_submission, user_operation_log, project_workshop
- **Resource**: resource
- **Admin**: admin, token_blacklist
- **Scheduled**: scheduled_task_log
