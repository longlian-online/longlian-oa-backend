# 开发说明

## 容器开发环境

项目配置有容器开发环境（.devcontainer）

## 配置

应用配置只走 YAML，不再读取 `.env`。

1. 复制模板（`application.yml` 已被 gitignore，不要提交）：

```
cp app/src/main/resources/application.yml.example app/src/main/resources/application.yml
```

2. 按本机环境改数据库、Redis、JWT、邮件等。每个字段的含义和取值见模板内注释。

Docker 一键开发环境（`task dev`）挂载本地 `application.yml`，再挂载 `application-dev.yml` 作为开发 profile 覆盖；Spring Boot 按属性优先级合并二者，并用 Compose 环境变量把 MySQL/Redis 指到容器服务名。

生产（`task prod`）部署迁移、后端和 VictoriaLogs，MySQL/Redis 用外部实例：

```
cp devops/application-prod.yml.example devops/application-prod.yml
```

YAML 挂到应用和迁移容器的 `config/application.yml`。不要把密钥打进镜像或提交到 git。

生产 VictoriaLogs 数据保存在 `devops/data/victorialogs`，查询端口仅绑定 `127.0.0.1:9428`；远程运维通过 SSH 端口转发访问，不要直接暴露到公网。

存储、CDN 等项直接写在 YAML 中。CDN 接入与 Type D 签名见 [cdn.md](cdn.md)。

## 运行

`mvn spring-boot:run -pl app`

## 可选本地日志采集

需要 Docker Desktop 和 Task。`task dev-observability` 在开发环境基础上启动 VictoriaLogs；Java Agent 通过 OTLP/HTTP 直接写入，导出配置见 [otel-agent.md](otel-agent.md)。

```bash
task dev-observability
docker compose \
  -f devops/docker-compose.dev.yml \
  -f devops/docker-compose.observability.yml ps
```

VictoriaLogs 查询页面：<http://127.0.0.1:9428/select/vmui/>。数据保存在被 Git 忽略的 `data/victorialogs`。

### 查询日志

从 VictoriaLogs `vlutils` 发布包取得 Windows 版本的 `vlogscli-prod.exe`，放到 `devops/tools/vlogscli-prod.exe`；也可使用 `devops/tools/vlutils-extract/vlogscli-windows-amd64-prod.exe`。直接启动：

```powershell
& .\devops\tools\vlogscli-prod.exe '-datasource.url=http://127.0.0.1:9428/select/logsql/query'
```

查询须以分号结束：

```text
_time:15m service.name:longlian-oa | limit 20;
_time:1h error | limit 50;
\tail _time:5m error;
```

脚本可通过标准输入调用官方 CLI：

```powershell
$query = '_time:15m service.name:longlian-oa | fields _time, _msg, service.name, deployment.environment | limit 20;'
$query | & .\devops\tools\vlogscli-prod.exe `
  '-datasource.url=http://127.0.0.1:9428/select/logsql/query' `
  '-loggerOutput=stdout'
```

访问开发应用后查询最近日志，确认其中含有 `service.name`、`deployment.environment`、`trace_id` 或 `span_id`。停止服务但保留数据使用 `task dev-observability-down`；需要重建空库时删除 `data/victorialogs`，不要在共享环境执行。

## ORM 代码生成

本项目依赖 Mybatis-Plus 代码生成器(generator)，默认不覆盖旧代码，在新增功能和表结构字段改动时需用到代码生成

1. 当新增新表时，运行生成器，将生成对应的 Controller、Service、Mapper 相关代码

2. 当表结构发生变动时，由于默认不覆盖旧代码，所以需要删除原有的 entity 文件夹（或对应的实体类代码文件），重新生成即可，其他代码不受影响

3. 数据库中枚举统一使用 TINYINT 类型，生成代码前，应在 app\src\main\java\online\longlian\app\common\enumeration 中定义枚举类型，并使用 ModelEnum 注解声明对应的表和字段，以便代码生成器为枚举字段生成正确的类型声明 