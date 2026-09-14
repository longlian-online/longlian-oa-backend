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

Docker 一键开发环境（`task dev`）挂载 `devops/application-dev.yml.example`，连接 compose 网络内的 MySQL/Redis，不必再配 `.env`。

生产（`task prod`）只部署迁移 + 后端，MySQL/Redis 用外部实例：

```
cp devops/application-prod.yml.example devops/application-prod.yml
cp devops/docker-compose.prod.override.yml.example devops/docker-compose.prod.override.yml
```

YAML 挂到容器 `config/application.yml`。Atlas 仍要 `DB_URL` / `DEV_DB_URL`，写在 override 里，不要把密钥打进镜像或提交到 git。

## 运行

`mvn spring-boot:run -pl app`

## ORM 代码生成

本项目依赖 Mybatis-Plus 代码生成器(generator)，默认不覆盖旧代码，在新增功能和表结构字段改动时需用到代码生成

1. 当新增新表时，运行生成器，将生成对应的 Controller、Service、Mapper 相关代码

2. 当表结构发生变动时，由于默认不覆盖旧代码，所以需要删除原有的 entity 文件夹（或对应的实体类代码文件），重新生成即可，其他代码不受影响

3. 数据库中枚举统一使用 TINYINT 类型，生成代码前，应在 app\src\main\java\online\longlian\app\common\enumeration 中定义枚举类型，并使用 ModelEnum 注解声明对应的表和字段，以便代码生成器为枚举字段生成正确的类型声明 