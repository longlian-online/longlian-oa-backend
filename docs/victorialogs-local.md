# 本地 VictoriaLogs 日志链路

## 启动

在仓库根目录执行：

```powershell
$env:LOG_QUERY_AUTH_TOKEN = "local-dev-token"
docker compose -f devops/docker-compose.dev.yml -f devops/docker-compose.observability.yml up --build -d
```

本地服务：

| 服务 | 地址 | 用途 |
|---|---|---|
| longlian-oa | http://127.0.0.1:10003 | 应用 |
| VictoriaLogs | http://127.0.0.1:9428 | 日志存储和查询 |
| Collector health | http://127.0.0.1:13133 | 采集器健康检查 |
| log-query-gateway | http://127.0.0.1:18080 | AI 只读日志查询入口 |

应用日志同时写入 `data/logs` 便于本地排查；Java Agent 通过 OTLP/gRPC 将应用日志和 traces 发给 Collector，Collector 再通过 OTLP/HTTP 转发日志到 VictoriaLogs。

## 使用 vlogscli

从 VictoriaLogs 的 `vlutils` 发布包获取 `vlogscli-prod.exe`，放到：

```text
devops/tools/vlogscli-prod.exe
```

启动：

```powershell
.\devops\vlogscli.ps1
```

进入交互界面后执行（查询末尾的 `;` 必须保留）：

```text
_time:15m;
```

也可以搜索应用启动日志：

```text
_time:15m longlian-oa;
```

Windows 下建议使用 `127.0.0.1`，不要改成 `localhost`，避免 CLI 解析本机 IPv6 地址失败。

退出：

```text
q
```

## HTTP 对照查询

```powershell
curl.exe "http://127.0.0.1:9428/select/logsql/query" `
  --data-urlencode "query=_time:15m longlian-oa"
```

## 使用 AI CLI 查询

本地网关默认使用 `local-dev-token`，也可以通过 `LOG_QUERY_AUTH_TOKEN` 覆盖。

```powershell
.\devops\ai-log-cli.ps1 `
  -Token "local-dev-token" `
  -Service "longlian-oa" `
  -Environment "dev" `
  -Since "15m" `
  -Limit 50
```

## 验收流程

1. `docker compose ps` 显示应用、Collector、VictoriaLogs、log-query-gateway 已启动。
2. `data/logs/app-*.log` 出现应用日志。
3. Collector health 返回成功。
4. vlogscli 能查询到 `longlian-oa` 日志。
5. 网关 health 返回成功。
6. AI CLI 查询结果与 vlogscli 结果一致。
7. 查询结果包含 `service.name`、`deployment.environment`、`trace_id`、`span_id` 等 OTLP 字段。
8. 重启 VictoriaLogs 后，`data/victorialogs` 中的数据仍可查询。

当前日志正文仍保留 Logback 原始文本，事件时间使用 OTLP 日志时间；后续如需兼容未接入 Java Agent 的服务，再增加独立的 filelog 管线，避免和 OTLP 日志重复采集。

## 停止

```powershell
docker compose -f devops/docker-compose.dev.yml -f devops/docker-compose.observability.yml down
```

不要使用 `down -v`，否则会删除 MySQL、应用存储等本地卷。
