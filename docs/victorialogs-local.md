# 本地 VictoriaLogs 日志链路

## 目标

本地开发环境使用以下链路验证应用日志：

```text
longlian-oa
  -> OpenTelemetry Java Agent
  -> OpenTelemetry Collector
  -> VictoriaLogs
  -> vlogscli
```

项目不实现日志查询服务。VictoriaLogs 自带 HTTP 查询 API 和 `vlogscli`，查询直接使用官方 CLI。

## 一键启动

需要 Docker Desktop 和 Task：

```bash
task dev-observability
```

该任务会启动：

- MySQL、Redis、Atlas 迁移；
- longlian-oa 开发容器；
- OpenTelemetry Collector；
- VictoriaLogs。

VictoriaLogs 数据保存在 `data/victorialogs`，该目录已被 Git 忽略。

查看服务状态：

```bash
docker compose \
  -f devops/docker-compose.dev.yml \
  -f devops/docker-compose.observability.yml ps
```

VictoriaLogs 查询页面：

```text
http://127.0.0.1:9428/select/vmui/
```

停止服务但保留数据：

```bash
task dev-observability-down
```

## vlogscli

从 VictoriaLogs `vlutils` 发布包获取 Windows 版本的 `vlogscli-prod.exe`，放到：

```text
devops/tools/vlogscli-prod.exe
```

也可以使用 `devops/tools/vlutils-extract/vlogscli-windows-amd64-prod.exe`。

使用官方 CLI 直接启动：

```powershell
& .\devops\tools\vlogscli-prod.exe '-datasource.url=http://127.0.0.1:9428/select/logsql/query'
```

CLI 默认连接：

```text
http://127.0.0.1:9428/select/logsql/query
```

输入查询时以分号结束；`;>` 是 CLI 的交互提示符，不要复制到查询内容中。例如：

```text
_time:15m | limit 20;
```

查询应用日志：

```text
_time:15m service.name:longlian-oa | limit 20;
```

查看错误日志：

```text
_time:1h error | limit 50;
```

实时查看新日志：

```text
\tail _time:5m error;
```

AI 或脚本可以直接把查询通过标准输入交给官方 CLI，不需要项目内的查询模块：

```powershell
$query = '_time:15m service.name:longlian-oa | fields _time, _msg, service.name, deployment.environment | limit 20;'
$query | & .\devops\tools\vlogscli-prod.exe `
  '-datasource.url=http://127.0.0.1:9428/select/logsql/query' `
  '-loggerOutput=stdout'
```

## 验证日志上报

1. 启动 `task dev-observability`。
2. 访问开发应用接口或等待应用启动日志产生。
3. 查看 Collector 日志，确认没有 exporter 错误：

```powershell
docker compose -f devops/docker-compose.dev.yml -f devops/docker-compose.observability.yml logs otel-collector
```

4. 使用上面的官方 CLI 命令启动查询。
5. 查询最近日志，并确认包含 `service.name`、`deployment.environment`、`trace_id` 或 `span_id` 字段。

Java Agent 通过 OTLP Logs 将 Logback 日志发送到 Collector，Collector 再通过
`/insert/opentelemetry/v1/logs` 写入 VictoriaLogs。

## 清理

停止服务后，如需重新验证空库，可以删除本地 `data/victorialogs`；不要在共享环境执行此操作。
