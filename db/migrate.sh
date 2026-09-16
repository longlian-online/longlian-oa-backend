#!/usr/bin/env sh
# 数据库同步脚本（声明式）
# 用法: ./migrate.sh <dev|prod> [apply|plan|inspect]
#
# 连接:
#   DB_URL            目标库 mysql://user:pass@host:3306/db
#                     可省略：缺省时从 APPLICATION_YML 的 spring.datasource 推导
#   APPLICATION_YML   Spring YAML 路径，默认 /app/config/application.yml
#   DEV_DB_URL        可选。不设则在同一 MySQL 实例自动创建 {db}_atlas
#   SEED_FILE         可选。apply 成功后导入种子数据
#
# 示例:
#   export DB_URL="mysql://root:pass@127.0.0.1:3306/longlian_oa_dev"
#   ./db/migrate.sh dev plan
#   ./db/migrate.sh dev apply

set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENVIRONMENT="${1:-}"
ACTION="${2:-apply}"
ATLAS_IMAGE="arigaio/atlas:1.2.3"

usage() {
  echo "用法: $0 <dev|prod> [apply|plan|inspect]" >&2
}

urlencode() {
  printf '%s' "$1" | awk '
    BEGIN {
      for (i = 1; i < 256; i++) {
        hex[sprintf("%c", i)] = sprintf("%%%02X", i)
      }
    }
    {
      n = length($0)
      for (i = 1; i <= n; i++) {
        c = substr($0, i, 1)
        if (c ~ /[A-Za-z0-9._~-]/) printf "%s", c
        else printf "%s", hex[c]
      }
    }
  '
}

urldecode() {
  decoded=$(printf '%s' "$1" | sed 's/%/\\x/g')
  printf '%b' "$decoded"
}

# 从 spring.datasource 抽出 url / username / password（本仓库 YAML 子集）
yaml_spring_datasource() {
  awk '
    function trim(s) {
      sub(/^[[:space:]]+/, "", s)
      sub(/[[:space:]]+$/, "", s)
      return s
    }
    function yamlval(s) {
      s = trim(s)
      if (s ~ /^".*"$/) return substr(s, 2, length(s) - 2)
      if (s ~ /^'\''.*'\''$/) return substr(s, 2, length(s) - 2)
      if (match(s, /[[:space:]]+#/)) s = substr(s, 1, RSTART - 1)
      return trim(s)
    }
    {
      sub(/\r$/, "")
      if ($0 ~ /^[[:space:]]*(#|$)/) next
      if ($0 ~ /^[^[:space:]#]/) {
        spring = ($0 ~ /^spring:[[:space:]]*$/)
        ds = 0
        next
      }
      if (!spring) next
      if ($0 ~ /^  [^[:space:]#]/) {
        ds = ($0 ~ /^  datasource:[[:space:]]*$/)
        next
      }
      if (!ds) next
      if (match($0, /^    url:[[:space:]]*/)) url = yamlval(substr($0, RSTART + RLENGTH))
      else if (match($0, /^    username:[[:space:]]*/)) user = yamlval(substr($0, RSTART + RLENGTH))
      else if (match($0, /^    password:[[:space:]]*/)) pass = yamlval(substr($0, RSTART + RLENGTH))
    }
    END {
      if (url == "") exit 1
      printf "url=%s\n", url
      printf "user=%s\n", user
      printf "pass=%s\n", pass
    }
  ' "$1"
}

jdbc_to_atlas_url() {
  jdbc=$1
  user=$2
  pass=$3
  rest=${jdbc#jdbc:mysql://}
  if [ "$rest" = "$jdbc" ]; then
    echo "错误: 只支持 jdbc:mysql:// 数据源，收到: $jdbc" >&2
    return 1
  fi
  rest=${rest%%\?*}
  hostport=${rest%%/*}
  dbname=${rest#*/}
  if [ -z "$dbname" ] || [ "$dbname" = "$rest" ]; then
    echo "错误: JDBC URL 缺少数据库名" >&2
    return 1
  fi
  case "$hostport" in
    *:*)
      host=${hostport%:*}
      port=${hostport##*:}
      ;;
    *)
      host=$hostport
      port=3306
      ;;
  esac
  printf 'mysql://%s:%s@%s:%s/%s\n' "$(urlencode "$user")" "$(urlencode "$pass")" "$host" "$port" "$dbname"
}

field_from() {
  printf '%s\n' "$1" | sed -n "s/^$2=//p"
}

resolve_db_url() {
  if [ -n "${DB_URL:-}" ]; then
    return 0
  fi
  yml="${APPLICATION_YML:-/app/config/application.yml}"
  if [ ! -f "$yml" ]; then
    echo "错误: 需要 DB_URL，或把 Spring YAML 挂到 $yml" >&2
    return 1
  fi
  echo "==> 从 $yml 的 spring.datasource 推导 DB_URL"
  ds=$(yaml_spring_datasource "$yml") || {
    echo "错误: 无法从 $yml 读取 spring.datasource.url" >&2
    return 1
  }
  jdbc_url=$(field_from "$ds" url)
  jdbc_user=$(field_from "$ds" user)
  jdbc_pass=$(field_from "$ds" pass)
  DB_URL=$(jdbc_to_atlas_url "$jdbc_url" "$jdbc_user" "$jdbc_pass")
  export DB_URL
}

# 拆 mysql://user:pass@host:port/db ，凭据解码后放入 url_* 变量
parse_mysql_url() {
  raw=$1
  noquery=${raw%%\?*}
  body=${noquery#mysql://}
  if [ "$body" = "$noquery" ]; then
    echo "错误: 不是 mysql:// URL" >&2
    return 1
  fi
  case "$body" in
    *@*)
      auth=${body%%@*}
      hostpart=${body#*@}
      url_user=$(urldecode "${auth%%:*}")
      case "$auth" in
        *:*) url_pass=$(urldecode "${auth#*:}") ;;
        *) url_pass= ;;
      esac
      ;;
    *)
      echo "错误: mysql:// URL 缺少用户信息" >&2
      return 1
      ;;
  esac
  url_db=${hostpart#*/}
  hostport=${hostpart%%/*}
  if [ -z "$url_db" ] || [ "$url_db" = "$hostpart" ]; then
    echo "错误: mysql:// URL 缺少数据库名" >&2
    return 1
  fi
  case "$hostport" in
    *:*)
      url_host=${hostport%:*}
      url_port=${hostport##*:}
      ;;
    *)
      url_host=$hostport
      url_port=3306
      ;;
  esac
}

mysql_exec() {
  if ! command -v mysql >/dev/null 2>&1; then
    echo "错误: 需要支持 caching_sha2_password 的 MySQL 客户端才能连接数据库" >&2
    return 1
  fi
  parse_mysql_url "$1"
  shift
  MYSQL_PWD="$url_pass" mysql --protocol=TCP --get-server-public-key \
    -h "$url_host" -P "$url_port" -u "$url_user" \
    --connect-timeout=10 "$@"
}

ensure_mysql_database() {
  if ! command -v mysql >/dev/null 2>&1; then
    return 1
  fi
  parse_mysql_url "$1"
  echo "==> 确保影子库 \`$url_db\` 存在于 $url_host:$url_port"
  MYSQL_PWD="$url_pass" mysql --protocol=TCP --get-server-public-key \
    -h "$url_host" -P "$url_port" -u "$url_user" \
    --connect-timeout=10 \
    -e "CREATE DATABASE IF NOT EXISTS \`$url_db\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
}

derive_shadow_url() {
  url=$1
  prefix=${url%%\?*}
  suffix=
  case "$url" in
    *\?*) suffix="?${url#*\?}" ;;
  esac
  db=${prefix##*/}
  rest=${prefix%/*}
  printf '%s/%s_atlas%s\n' "$rest" "$db" "$suffix"
}

resolve_dev_url() {
  if [ -n "${DEV_DB_URL:-}" ]; then
    case "$DEV_DB_URL" in
      mysql://*)
        if ! ensure_mysql_database "$DEV_DB_URL"; then
          echo "错误: DEV_DB_URL 指向的库不存在且无法自动创建" >&2
          exit 1
        fi
        ;;
    esac
    export DEV_DB_URL
    return 0
  fi

  derived=$(derive_shadow_url "$DB_URL")
  if ensure_mysql_database "$derived"; then
    DEV_DB_URL=$derived
    export DEV_DB_URL
    return 0
  fi

  parse_mysql_url "$derived"
  echo "错误: 无法自动创建影子库 \`$url_db\`。" >&2
  echo "请授予 CREATE DATABASE，或预先建好该库并设置 DEV_DB_URL。" >&2
  exit 1
}

rewrite_localhost_for_docker() {
  printf '%s' "$1" | sed 's/@localhost/@host.docker.internal/g; s/@127.0.0.1/@host.docker.internal/g'
}

setup_atlas() {
  if command -v atlas >/dev/null 2>&1; then
    run_atlas() {
      atlas "$@"
    }
    return 0
  fi
  if ! command -v docker >/dev/null 2>&1; then
    echo "错误: 需要安装 atlas CLI 或 docker" >&2
    exit 1
  fi
  docker_db_url=$(rewrite_localhost_for_docker "$DB_URL")
  run_atlas() {
    docker_dev_url=$(rewrite_localhost_for_docker "${DEV_DB_URL}")
    docker run --rm \
      -v "${SCRIPT_DIR}:/work" -w /work \
      --add-host host.docker.internal:host-gateway \
      -e "DB_URL=${docker_db_url}" \
      -e "DEV_DB_URL=${docker_dev_url}" \
      "${ATLAS_IMAGE}" "$@"
  }
}

seed_if_requested() {
  if [ -z "${SEED_FILE:-}" ]; then
    return 0
  fi
  if [ ! -f "$SEED_FILE" ]; then
    echo "错误: SEED_FILE 不存在: $SEED_FILE" >&2
    exit 1
  fi
  parse_mysql_url "$DB_URL"
  echo "==> 导入种子数据 $SEED_FILE"
  mysql_exec "$DB_URL" "$url_db" < "$SEED_FILE"
}

case "$ENVIRONMENT" in
  dev|prod) ;;
  *)
    usage
    exit 1
    ;;
esac

case "$ACTION" in
  apply|plan|inspect) ;;
  *)
    usage
    exit 1
    ;;
esac

resolve_db_url
setup_atlas
cd "$SCRIPT_DIR"

case "$ACTION" in
  apply)
    resolve_dev_url
    echo "==> [${ENVIRONMENT}] 同步数据库到 schema.sql 声明状态..."
    run_atlas schema apply --env "$ENVIRONMENT" --auto-approve
    echo "==> [${ENVIRONMENT}] 同步完成"
    seed_if_requested
    ;;
  plan)
    resolve_dev_url
    echo "==> [${ENVIRONMENT}] 计划将执行的变更（不改动数据库）..."
    run_atlas schema apply --env "$ENVIRONMENT" --dry-run
    ;;
  inspect)
    resolve_dev_url
    run_atlas schema inspect --env "$ENVIRONMENT"
    ;;
esac
