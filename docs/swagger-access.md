# API 文档访问

默认配置及 `prod` 环境关闭 Swagger UI 和 OpenAPI 文档。开发时显式设置
`SPRING_PROFILES_ACTIVE=dev` 或 `local`；`SPRINGDOC_ENABLED=false` 可继续关闭文档。

部署时检查 `SPRING_PROFILES_ACTIVE` 和 `SPRINGDOC_ENABLED`。若生产环境显式开启文档，
必须先在网关对 `/swagger-ui.html`、`/swagger-ui/**`、`/v3/api-docs/**` 设置访问控制；
应用内这些路径仍用于开发环境的匿名文档访问。
