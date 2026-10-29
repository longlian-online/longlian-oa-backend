<p align="center">
  <img src="./docs/logo-full.webp" alt="白箱" width="96" />
</p>

<h1 align="center">白箱</h1>

<p align="center">LongLian OA 的服务端。</p>

白箱后端是 LongLian OA 的 API 服务，负责身份认证、数据持久化和管理接口。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 框架 | Spring Boot 3.3.5 |
| 语言 | Java 21 |
| 数据 | MariaDB 10.11、MyBatis Plus 3.5.15 |
| 认证 | Spring Security、JWT |
| 缓存 | Redis |
| 接口文档 | Springdoc OpenAPI |

## 本地开发

环境、配置和启动方式见 [docs/dev.md](docs/dev.md)。数据库结构见 [db/DEVELOPMENT.md](db/DEVELOPMENT.md)。

## 许可证

本项目以 [Apache License 2.0](./LICENSE) 授权。

Copyright 2025-2026 longlian-online
