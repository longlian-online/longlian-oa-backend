// Atlas 配置 — 声明式数据库管理
// 文档: https://atlasgo.io/concepts/config
//
// 声明式模式：schema.sql 为唯一真实来源，
// atlas schema apply 自动计算差异并同步数据库。

variable "db_url" {
  type        = string
  description = "目标数据库连接地址"
  default     = "mysql://root:dev@localhost:10001/longlian_oa_dev"
}

variable "dev_db_url" {
  type        = string
  description = "开发数据库地址（用于 diff 计算，需要可创建临时 schema）"
  default     = "mysql://root:dev@localhost:10001/dev"
}

env "local" {
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

// 生产环境：通过环境变量注入数据库地址
env "prod" {
  src = "file://schema.sql"
  dev = "mysql://root:${MYSQL_ROOT_PASSWORD}@${MYSQL_HOST}:${MYSQL_PORT}/dev"
  url = "mysql://root:${MYSQL_ROOT_PASSWORD}@${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}"

  diff {
    skip {
      drop_schema = true
      drop_table  = true
    }
  }
}
