// Atlas 配置 — 声明式数据库管理
// 文档: https://atlasgo.io/concepts/config
//
// 声明式模式：schema.sql 为唯一真实来源，
// atlas schema apply 自动计算差异并同步数据库。

variable "db_url" {
  type        = string
  description = "目标数据库连接地址"
  default     = getenv("DB_URL")
}

variable "dev_db_url" {
  type        = string
  description = "开发数据库地址（用于 diff 计算，需要可创建临时 schema）"
  default     = getenv("DEV_DB_URL")
}

env "local" {
  src = "file://schema.sql"
  dev = var.dev_db_url
  url = var.db_url
}

// 生产环境：通过环境变量注入数据库地址
env "prod" {
  src = "file://schema.sql"
  dev = var.dev_db_url
  url = var.db_url

  diff {
    skip {
      drop_schema = true
      drop_table  = true
    }
  }
}
