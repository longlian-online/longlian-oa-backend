// Atlas 配置 — 声明式数据库管理。
// schema.sql 是唯一真实来源；dev 可完整收敛，prod 不自动删除已有对象。

variable "db_url" {
  type        = string
  description = "目标数据库连接地址"
  default     = getenv("DB_URL")
}

variable "dev_db_url" {
  type        = string
  description = "Atlas 暂存数据库地址（用于 diff 计算，必须是可丢弃的专用空库）"
  default     = getenv("DEV_DB_URL")
}

// 开发环境允许删除废弃对象，使数据库完整收敛到 schema.sql。
env "dev" {
  src = "file://schema.sql"
  dev = var.dev_db_url
  url = var.db_url
}

// 生产环境不自动删除对象，避免 schema.sql 的误删直接造成数据丢失。
env "prod" {
  src = "file://schema.sql"
  dev = var.dev_db_url
  url = var.db_url

  diff {
    skip {
      drop_schema      = true
      drop_table       = true
      drop_column      = true
      drop_index       = true
      drop_foreign_key = true
    }
  }
}
