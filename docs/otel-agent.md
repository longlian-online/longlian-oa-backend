# OpenTelemetry Agent 配置说明

## 概述

项目通过 Docker 镜像支持 OpenTelemetry Java Agent 链路追踪。采用「构建时打包 + 运行时启用」的分离策略：

- **构建时**：通过 `INCLUDE_OTEL` 构建参数决定是否将 agent jar 打入镜像
- **运行时**：通过标准 JVM 环境变量 `JAVA_TOOL_OPTIONS` 决定是否启用 agent

构建参数只控制 agent 文件是否存在，不会自动开启链路追踪。

## 构建镜像

### 不含 agent（默认，精简镜像）

```bash
docker build -f devops/Dockerfile -t longlian-oa:default .
```

### 含 agent

```bash
docker build --build-arg INCLUDE_OTEL=true -f devops/Dockerfile -t longlian-oa:otel .
```

agent 版本：`v2.26.1`，文件位于镜像内 `/app/opentelemetry-javaagent.jar`。

构建阶段会使用 `curl -fL` 下载文件，并通过 `jar tf` 校验其确实是有效的 JAR；下载失败或校验失败会使镜像构建失败。未启用 agent 时，构建阶段不会下载该文件。

## 运行时启用

含 agent 的镜像无需修改启动命令，通过 `JAVA_TOOL_OPTIONS` 环境变量注入即可，JVM 启动时会自动读取：

```bash
docker run \
  -e JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar" \
  -e OTEL_SERVICE_NAME=longlian-oa \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317 \
  longlian-oa:otel
```

### docker-compose 示例

```yaml
services:
  app:
    image: longlian-oa:otel
    environment:
      JAVA_TOOL_OPTIONS: "-javaagent:/app/opentelemetry-javaagent.jar"
      OTEL_SERVICE_NAME: longlian-oa
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
```

## 常用 OTel 环境变量

| 变量 | 说明 | 示例 |
|---|---|---|
| `OTEL_SERVICE_NAME` | 服务名称 | `longlian-oa` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP Collector 地址 | `http://collector:4317` |
| `OTEL_TRACES_SAMPLER` | 采样策略 | `always_on` / `traceidratio` |
| `OTEL_TRACES_SAMPLER_ARG` | 采样参数（ratio 时为 0~1） | `0.5` |
| `OTEL_RESOURCE_ATTRIBUTES` | 附加资源属性 | `deployment.environment=prod` |

完整配置参考：[OpenTelemetry SDK 环境变量文档](https://opentelemetry.io/docs/languages/sdk-configuration/)

## 启动保护

如果使用不含 agent 的镜像，却仍通过 `JAVA_TOOL_OPTIONS` 配置了 `-javaagent`，Dockerfile 的 ENTRYPOINT 会自动移除该参数后再启动应用，避免 JVM 因找不到 agent 文件而直接退出。使用含 agent 的镜像时不会修改 `JAVA_TOOL_OPTIONS`。

因此，推荐只在 `INCLUDE_OTEL=true` 的镜像上设置 `JAVA_TOOL_OPTIONS=-javaagent:/app/opentelemetry-javaagent.jar`。

## 验证镜像

构建完成后，可以检查两种镜像的文件状态：

```bash
# 含 agent：文件应存在且非空
docker run --rm --entrypoint sh longlian-oa:otel \
  -c 'test -s /app/opentelemetry-javaagent.jar'

# 不含 agent：文件不应存在
docker run --rm --entrypoint sh longlian-oa:default \
  -c '! test -e /app/opentelemetry-javaagent.jar'
```
