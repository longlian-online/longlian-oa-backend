# MCP 接入

## 入口与分层

使用 Spring AI MCP Server WebMVC，通过 Streamable HTTP 提供 `/mcp`。
`online.longlian.app.mcp` 与 `controller` 平级，只负责工具声明、参数适配和调用 Service；不通过 Controller 或内部 HTTP 转发。

使用 starter 自带的 `@McpTool` 注解自动发现工具。无参数的 `ping` 工具返回文本 `pong`，不访问数据库、不操作组织或业务数据。任务查询由 `McpTaskQueryService` 聚合现有用户端 Service，接取、上传和暂存直接复用原有 Service。
工具 `ping` 通过 `tools/call` 调用，与 MCP 协议自带的 `ping` 方法不同。

## 客户端连接

- URL：后端基础地址加 `/mcp`。
- 传输：Streamable HTTP，不是旧版 `/sse`。
- 认证：在个人设置的组织设置中生成 API Key，每次请求携带 `Authorization: Bearer <API Key>`。完整 Key 仅在生成时显示；MCP 地址同时返回。
- API Key 绑定本人及一个组织，无需携带 `X-Org-Id`；若携带，必须与 Key 对应的组织一致。API Key 仅可访问 `/mcp`，不能调用网页 REST 接口。
- 兼容原有 JWT：沿用 JWT 的有效期和撤销校验；`ping` 不要求 `X-Org-Id`，业务工具仍必须携带该头。管理员令牌不能作为组员身份使用。
- MCP 认证失败返回 HTTP 401、权限失败返回 HTTP 403，不使用 REST Result。暂未实现 OAuth 授权发现或自动登录/刷新。
- HTTP 请求的认证信息、组织头及 API Key 绑定的 `SessionContext` 经 `McpTransportContext` 传给 SDK 执行线程；业务 Service 仍显式接收 `userId`、`orgId`，不读取线程上下文。
- JWT 和 API Key 统一由现有 `JwtAuthenticationFilter` 处理，沿用同一条安全链。`ll_mcp_` 前缀仅在 MCP 入口校验，成功后复用现有用户认证策略，不定义专用 Token；绑定组织保留在请求上下文，不写入用户缓存。无效 Key 不回退到 JWT 校验。

客户端先初始化连接，再使用 `tools/list` 发现工具，调用：

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": { "name": "ping", "arguments": {} }
}
```

返回的工具结果包含文本 `pong`，不使用 REST 接口的 `Result` 包装。SSE 响应的内部异步派发不重复鉴权；每个客户端新请求仍必须携带有效凭据，MCP 会话 ID 不能代替认证。
支持 Streamable HTTP 且允许自定义认证请求头的 MCP 客户端可以接入；仅支持 OAuth 的客户端不能直接使用此认证方式。

## API Key 管理接口

以下接口使用网页用户 JWT，均不要求 `X-Org-Id`，只能管理本人凭据。

| 方法 | 路径 | 返回 |
| --- | --- | --- |
| GET | `/app/user/organizations/api-key-settings` | 本人的组织选项，包含被禁用的组织或成员关系 |
| GET | `/app/user/organizations/{orgId}/api-keys/current` | 状态、可展示前缀、生成及撤销时间、操作权限，不返回原文 |
| POST | `/app/user/organizations/{orgId}/api-keys` | 生成或替换当前 Key，一次返回原文及 MCP 地址 |
| DELETE | `/app/user/organizations/{orgId}/api-keys/current` | 撤销当前 Key，可重复调用 |

状态为 `NOT_CREATED`、`ACTIVE`、`SUSPENDED`、`REVOKED`。重新生成和撤销后旧凭据不能继续发起请求；禁用账号、组织或成员后拒绝访问，恢复启用后未撤销的 Key 可继续使用。改密或重置密码会在同一事务内撤销本人全部 Key。成员退出、移除流程可调用 `revokeForMembership` 参与其事务；当前未提供退出或移除入口。

## 工具契约

工具结果以 `content[].text` 中的 JSON 返回，ID 使用十进制字符串。业务失败由 SDK 转为 `isError=true`，保留原异常信息，不使用 REST Result 包装。

| 工具 | 主要入参 | 返回内容 |
| --- | --- | --- |
| `find_projects` | `keyword?`、`pageNum?`、`pageSize?` | 企划 `projectId`、名称、别名和说明 |
| `list_project_items` | `projectId`、`keyword?`、`pageNum?`、`pageSize?` | 项目 `itemId`、名称和进度，按自定义顺序排列 |
| `list_my_tasks` | `bucket?`、`sortDirection?`、`pageNum?`、`pageSize?` | 当前组织内本人的已接取待办，含企划、项目和任务 ID |
| `list_claimable_tasks` | `baseTaskId?`、`keyword?`、`projectType?`、`sortBy?`、`stageScope?`、`pageNum?`、`pageSize?` | 当前组织的缺人任务，按企划分组分页 |
| `get_project_tasks` | `projectId`、`itemId` | 按阶段排序的任务，含接取人、草稿标记、可执行提示和前置阻塞 |
| `get_task` | `projectId`、`itemId`、`instanceId` | 提交字段、参考附件、前序正式产出、本人的原始暂存值和展示内容 |
| `claim_task` | `instanceId` | `autoJoinedProject`，表示本次是否自动加入企划 |
| `abandon_task` | `instanceId`、`expectedRevision` | `abandoned` 和任务 ID；只允许放弃本人任务，成功后清空暂存 |
| `get_task_file_url` | `source`、`ownerId`、`fileId` | 临时 `readUrl` 和 Unix 秒 `expireAt` |
| `create_task_upload` | `fileName`、`fileExt`、`fileSize`、`fileMime` | 字符串 `fileId`、`method=PUT`、`uploadUrl`、必须逐字使用的 `headers` |
| `save_task_draft` | `instanceId`、`values` | `saved` 和任务 ID |

分页默认第 1 页、每页 20 项，上限 100。`projectId` 表示企划，`itemId` 表示企划内的项目。
`list_my_tasks` 默认 `bucket=SUBMITTABLE`、`sortDirection=ASC`，可用 `WAITING` 查询等前置的待办，正式提交后不再列出。
`list_claimable_tasks` 默认 `stageScope=CURRENT`、`sortBy=WAIT`，`total` 为企划数；`ALL` 包含后续阶段，任务的 `isCurrentStage` 表示是否处于当前阶段。
`canClaim` 表示当前任务可接取，包含前置尚未完成时的预接取；`canWork` 表示本人已接取且前置全部完成，可以开始处理。`blockedBy` 列出未完成前置。按顺序执行时优先接取无阻塞任务，用户要求预接取时可先接取后续任务，随后等待前置完成。
枚举参数使用工具说明中的大写取值，非法值返回中文参数提示和允许值，例如「bucket 必须为 SUBMITTABLE 或 WAITING」。
放弃任务前从 `get_task` 的 `task.revision`（或 `get_project_tasks` 中对应任务的 `revision`）读取任务修订号，原样传为 `expectedRevision`。该修订号沿用现有任务状态校验，不是草稿版本。
只有用户明确要求放弃时才调用，须告知暂存会清空；修订号过期则重新读取，不自动重试。MCP 不提供负责人取消他人接取的操作。
`get_task_file_url` 的 `source=task` 使用任务实例 ID 和文件 ID；`source=project` 使用企划 ID 和 `references[].attachmentId`。
上传仅支持固定的 `task_submit`，客户端直接 PUT 文件；不返回独立的存储 key 或存储类型。
MCP 不提供正式提交或管理工具。工具注册表和服务端 instructions 定义调用顺序；不包含后台任务调度器。

## 配置与版本

MCP 默认配置位于 `app/src/main/resources/application.properties`，与环境 YAML 配置共同加载。可以通过环境变量 `SPRING_AI_MCP_SERVER_ENABLED=false` 关闭服务。

- Java 21
- Spring Boot 3.5.16
- Spring AI 1.1.8（BOM 管理）
- Springdoc 2.8.17（适配 Boot 3.5 / Spring Framework 6.2）
- Redisson 3.52.0（使用 Spring Data Redis 3.5 适配模块）

Boot starter 的版本统一由父 POM 管理，不单独固定 Web、Test 的旧版本。

## 验证

按仓库的 `task test-docker` 执行单元测试和 API 测试。
MCP API 测试覆盖初始化、工具契约、身份与组织校验、接取与暂存、网页正式提交后的流程变化，以及真实本地预签名文件上传与读取；不修改共享测试基类。
