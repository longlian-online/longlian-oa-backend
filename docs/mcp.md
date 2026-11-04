# MCP 接入

## 入口与分层

使用 Spring AI MCP Server WebMVC，通过 Streamable HTTP 提供 `/mcp`。
`online.longlian.app.mcp` 与 `controller` 平级，只负责工具声明、参数适配和调用 Service；不通过 Controller 或内部 HTTP 转发。

使用 starter 自带的 `@McpTool` 注解自动发现工具。当前只注册无参数的 `ping` 工具，返回文本 `pong`，不访问数据库、不操作组织或业务数据，因此不新增 Service。
工具 `ping` 通过 `tools/call` 调用，与 MCP 协议自带的 `ping` 方法不同。

## 客户端连接

- URL：后端基础地址加 `/mcp`。
- 传输：Streamable HTTP，不是旧版 `/sse`。
- 认证：每次请求携带 `Authorization: Bearer <用户访问凭证>`。
- 沿用现有 JWT 的有效期和撤销校验；MCP 认证失败返回 HTTP 401、权限失败返回 HTTP 403，不使用 REST Result。暂未实现 OAuth 授权发现或自动登录/刷新。
- `ping` 不要求 `X-Org-Id`；这不代表后续业务工具可以省略组织授权。

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
MCP API 测试覆盖初始化、工具发现和调用，以及未认证和无效凭证请求；不修改共享测试基类。
