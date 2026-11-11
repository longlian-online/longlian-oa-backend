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
    │   ├── notification/   # 通知创建、来源解析和已读状态
    │   └── deletion/       # 删除申请
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

## 定时任务

| 路径 | 说明 |
|------|------|
| `scheduled/ScheduledTask.java` | 任务接口，所有定时任务实现此接口 |
| `scheduled/ScheduledTaskEngine.java` | 调度引擎，启动时扫描所有 ScheduledTask Bean 并注册 cron 任务 |
| `scheduled/task/` | 任务实现目录（如 HeartbeatTask） |
| `service/scheduled/` | 任务日志服务 |
| `controller/admin/ScheduledTaskController.java` | 手动触发定时任务 API |
| `pojo/bo/common/ScheduledTaskDefinitionBO.java` | 任务定义 BO |
| `pojo/entity/ScheduledTaskLog.java` | 任务执行日志实体 |

## 数据库

数据库使用 Atlas 声明式管理，文件位于根目录 `db/`

- 期望状态：`db/schema.sql`（唯一真实来源）
- CI 脚本：`db/migrate.sh`
- 开发文档：`db/DEVELOPMENT.md`

主要表结构：
- **RBAC**: permission, role, role_permission, user, user_role
- **Organization**: organization, organization_member, group_application
- **OTP**: one_time_password, email_verify_otp, organization_create_otp, organization_join_otp
- **Project/Work**: project, project_type, item, base_task, task_template, task_template_node, item_task_flow, item_task_node, task_instance, task_submission, user_operation_log, project_workshop
- **Resource**: resource
- **Admin**: admin, token_blacklist
- **Scheduled**: scheduled_task_log

## 任务大厅查询

`GET /app/task/instance/plaza` 按企划分页；`stageScope=CURRENT`（默认）只返回首个未完成阶段的待接取任务，`ALL` 返回所有阶段的待接取任务。
`isCurrentStage` 标记任务是否位于首个未完成阶段；后续阶段可以通过现有 claim 接口提前认领。
组织隔离、进行中企划/条目及软删除过滤在两种视图下保持一致。组内任务全量返回，前端默认展示 6 项并支持展开。
