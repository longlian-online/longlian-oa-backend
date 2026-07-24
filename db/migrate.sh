#!/usr/bin/env sh
# CI 数据库同步脚本（声明式）
# 用法: ./migrate.sh [apply|diff|inspect]
#
# 环境变量:
#   DB_URL       - 目标数据库连接地址 (必须)
#   DEV_DB_URL   - 开发数据库地址，用于 diff 计算 (必须)
#
# 示例:
#   DB_URL="mysql://root:pass@db:3306/mydb" DEV_DB_URL="mysql://root:pass@db:3306/dev" ./migrate.sh apply
#   DB_URL="mysql://root:pass@db:3306/mydb" DEV_DB_URL="mysql://root:pass@db:3306/dev" ./migrate.sh diff

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ACTION="${1:-apply}"

# 检查 Atlas 是否可用（优先本地 CLI，否则用 Docker）
if command -v atlas >/dev/null 2>&1; then
  ATLAS="atlas"
elif command -v docker >/dev/null 2>&1; then
  ATLAS="docker run --rm -v ${SCRIPT_DIR}:/work -w /work --network=host arigaio/atlas:latest"
else
  echo "错误: 需要安装 atlas CLI 或 docker" >&2
  exit 1
fi

check_env() {
  if [ -z "$DB_URL" ]; then
    echo "错误: 需要设置 DB_URL 环境变量" >&2
    exit 1
  fi
  if [ -z "$DEV_DB_URL" ]; then
    echo "错误: 需要设置 DEV_DB_URL 环境变量" >&2
    exit 1
  fi
}

case "$ACTION" in
  apply)
    check_env
    echo "==> 同步数据库到 schema.sql 声明状态..."
    $ATLAS schema apply --env ci --url "$DB_URL" --dev-url "$DEV_DB_URL" --auto-approve
    echo "==> 同步完成"
    ;;
  diff)
    check_env
    echo "==> 计算差异..."
    $ATLAS schema diff --env ci --url "$DB_URL" --dev-url "$DEV_DB_URL"
    ;;
  inspect)
    check_env
    $ATLAS schema inspect --env ci --url "$DB_URL"
    ;;
  *)
    echo "用法: $0 [apply|diff|inspect]" >&2
    exit 1
    ;;
esac
