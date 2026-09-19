# AI 日志查询网关

## 架构

```text
AI CLI / 后续 MCP
        │ HTTPS（Nginx）
        ▼
log-query-gateway :18080
        │ 内网 HTTP
        ▼
VictoriaLogs :9428
```

Nginx将一个 HTTPS 路由转发到服务器本机的 `127.0.0.1:18080/api/v1/logs/search`；外部路径可以直接沿用 `/api/v1/logs/search`，也可以使用自定义路径并在 Nginx 侧改写。网关只绑定回环地址，VictoriaLogs 也只绑定回环地址，避免绕过 Nginx 直接暴露查询入口。

网关是只读查询面：不接收日志、不提供 VictoriaLogs 原始管理接口、不执行任意 HTTP URL。它固定调用容器网络中的 `/select/logsql/query`，并对服务名、环境、时间范围、条数和并发数做限制。

## 服务器启动

准备生产配置后，在仓库根目录执行：

```bash
cp devops/application-prod.yml.example devops/application-prod.yml
# 填写 application-prod.yml
export LOG_QUERY_AUTH_TOKEN='替换为随机长令牌'
task prod-observability
```

等服务健康后检查：

```bash
docker compose -f devops/docker-compose.prod.yml \
  -f devops/docker-compose.observability.prod.yml ps
curl http://127.0.0.1:18080/actuator/health
```

生产叠加文件包含：

- `victorialogs`：单节点持久化目录 `devops/data/victorialogs`；
- `otel-collector`：OTLP Gateway，接收应用日志并通过 OTLP/HTTP 写入 VictoriaLogs；
- `log-query-gateway`：构建 `devops/Dockerfile.log-query-gateway`，监听宿主机回环地址 `18080`；
- `longlian-oa`：通过 Java Agent 将 OTLP 日志发送至 Collector。

Collector 的队列落盘在 `devops/data/otel-collector`。生产 Compose 会先用一次性权限初始化容器将该目录交给 Collector 的 `10001:10001` 运行用户，并保持目录为 `0700`，避免本机普通用户绕过网关读取日志或重试队列。备份 VictoriaLogs 数据目录和 Collector 队列目录时，应一并纳入服务器备份策略。

## AI CLI

Windows PowerShell：

```powershell
.\devops\ai-log-cli.ps1 `
  -GatewayUrl "https://已有Nginx域名/internal/logs/search" `
  -Token "$env:LOG_QUERY_AUTH_TOKEN" `
  -Service "longlian-oa" `
  -Environment "prod" `
  -Since "15m" `
  -Keyword "请求资源不存在" `
  -Limit 50 `
  -Format markdown
```

Linux/macOS：

```bash
# 依赖 jq，用于安全编码包含引号、换行等字符的 JSON 参数
./devops/ai-log-cli.sh \
  --token "$LOG_QUERY_AUTH_TOKEN" \
  --service longlian-oa \
  --environment prod \
  --since 15m \
  --keyword '请求资源不存在' \
  --limit 50
```

网关接口为 `POST /api/v1/logs/search`，请求示例：

```json
{
  "service": "longlian-oa",
  "environment": "prod",
  "keyword": "请求资源不存在",
  "traceId": "0123456789abcdef0123456789abcdef",
  "since": "15m",
  "limit": 50
}
```

`since` 和 `from`/`to` 只能二选一；默认查询最近 15 分钟，最大 24 小时，最大返回 200 条。返回结果会对 password、token、secret、authorization、cookie、API key 等字段及日志正文中的同类键值进行脱敏。

## Nginx 对接约定

Nginx 侧只需完成以下职责：

1. 终止 HTTPS；
2. 将查询路由转发到 `http://127.0.0.1:18080`；
3. 保留 `Authorization`、`Content-Type`、`X-Request-Id`；
4. 按现有安全策略限制来源、请求体大小和请求频率。

认证令牌由 Nginx 透传，网关再次校验 `Authorization: Bearer <LOG_QUERY_AUTH_TOKEN>`。后续 MCP 直接复用该 HTTPS 查询地址和请求模型，不需要连接 VictoriaLogs，也不需要修改 Collector。

## 本地验证

本地开发叠加配置仍使用：

```powershell
$env:LOG_QUERY_AUTH_TOKEN = "local-dev-token"
docker compose -f devops/docker-compose.dev.yml `
  -f devops/docker-compose.observability.yml up --build -d
```

本地叠加配置会同时启动网关，并将宿主机 `127.0.0.1:18080` 映射到容器 8080。网关通过 Docker 内部地址 `http://victorialogs:9428/select/logsql/query` 查询 VictoriaLogs。人工直接查询 VictoriaLogs 仍可使用现有 `vlogscli`；AI CLI 只使用网关。
