# OpenTelemetry Agent 配置说明

## 概述

项目通过 Docker 镜像支持 OpenTelemetry Java Agent 链路追踪。采用「构建时打包 + 运行时启用」的分离策略：

- **构建时**：通过 `INCLUDE_OTEL` 构建参数决定是否将 agent jar 打入镜像
- **运行时**：通过标准 JVM 环境变量 `JAVA_TOOL_OPTIONS` 决定是否启用 agent

## 构建镜像

### 不含 agent（默认，精简镜像）

```bash
docker build -f devops/Dockerfile -t longlian-oa .
```

### 含 agent

```bash
docker build --build-arg INCLUDE_OTEL=true -f devops/Dockerfile -t longlian-oa .
```

agent 版本：`v2.26.1`，文件位于镜像内 `/app/opentelemetry-javaagent.jar`。

## 运行时启用

无需修改镜像或启动命令，通过 `JAVA_TOOL_OPTIONS` 环境变量注入即可，JVM 启动时会自动读取：

```bash
docker run \
  -e JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar" \
  -e OTEL_SERVICE_NAME=longlian-oa \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317 \
  longlian-oa
```

### docker-compose 示例

```yaml
services:
  app:
    image: longlian-oa
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

## 设计说明

- 构建时未设置 `INCLUDE_OTEL=true` 则不下载 agent，镜像中不包含该文件（精简约 30MB）
- Dockerfile 中使用 glob 模式 `opentelemetry-javaagent.ja[r]` 进行 COPY，文件不存在时静默跳过，不会导致构建失败
- ENTRYPOINT 不包含任何 OTel 逻辑，保持简洁；启用完全由运行时环境变量控制
