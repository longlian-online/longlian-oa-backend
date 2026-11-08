# MCP 接入

## 入口与分层

使用 Spring AI MCP Server WebMVC，通过 Streamable HTTP 提供 `/mcp`。
`online.longlian.app.mcp` 与 `controller` 平级，只负责工具声明、参数适配和调用 Service；不通过 Controller 或内部 HTTP 转发。

使用 starter 自带的 `@McpTool` 注解自动发现工具。无参数的 `ping` 工具返回文本 `pong`，不访问数据库、不操作组织或业务数据。任务查询由 `McpTaskQueryService` 聚合现有用户端 Service，接取、上传和暂存直接复用原有 Service。
工具 `ping` 通过 `tools/call` 调用，与 MCP 协议自带的 `ping` 方法不同。

## 客户端连接

- URL：后端基础地址加 `/mcp`。
- 传输：Streamable HTTP，不是旧版 `/sse`。
- 认证：每次请求携带 `Authorization: Bearer <用户访问凭证>`。
- 沿用现有 JWT 的有效期和撤销校验；MCP 认证失败返回 HTTP 401、权限失败返回 HTTP 403，不使用 REST Result。暂未实现 OAuth 授权发现或自动登录/刷新。
- `ping` 不要求 `X-Org-Id`；业务工具每次请求必须携带 `X-Org-Id: <当前组织 ID>`，并要求本人为该组织启用成员。管理员令牌不能作为组员身份使用。
- HTTP 请求的认证信息和组织头经 `McpTransportContext` 传给 SDK 执行线程；业务 Service 仍显式接收 `userId`、`orgId`，不读取线程上下文。

客户端先初始化连接，再使用 `tools/list` 发现工具，调用：

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": { "name": "ping", "arguments": {} }
}
```

返回的工具结果包含文本 `pong`，不使用 REST 接口的 `Result` 包装。SSE 响应的内部异步派发不重复鉴权；每个客户端新请求仍必须携带有效 JWT，MCP 会话 ID 不能代替认证。
支持 Streamable HTTP 且允许自定义认证请求头的 MCP 客户端可以接入；仅支持 OAuth 的客户端不能直接使用此认证方式。

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
