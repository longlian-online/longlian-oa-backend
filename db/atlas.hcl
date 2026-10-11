// Atlas 配置 — 声明式数据库管理。
// schema.sql 是唯一真实来源；所有环境保留旧字段，prod 还保护其他已有对象。
// DB_URL / DEV_DB_URL 由 migrate.sh 注入（YAML 推导；默认同实例 {db}_atlas）。

variable "db_url" {
  type        = string
  description = "目标数据库连接地址"
  default     = getenv("DB_URL")
}

variable "dev_db_url" {
  type        = string
  description = "Atlas 暂存库（migrate.sh 注入，默认同实例 {db}_atlas）"
  default     = getenv("DEV_DB_URL")
}

// 开发环境保留旧字段，便于结构同步后迁移历史数据。
env "dev" {
  src = "file://schema.sql"
  dev = var.dev_db_url
  url = var.db_url

  diff {
    skip {
      drop_column = true
    }
  }
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
