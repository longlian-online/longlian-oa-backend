#!/usr/bin/env sh
# 数据库同步脚本（声明式）
# 用法: ./migrate.sh [apply|plan|inspect]
#
# 环境变量:
#   DB_URL       - 目标数据库连接地址 (必须)
#   DEV_DB_URL   - Atlas 暂存库地址 (必须，会被反复清空，须为专用空库)
#
# 示例:
#   export DB_URL="mysql://root:pass@db:3306/longlian_oa"
#   export DEV_DB_URL="mysql://root:pass@db:3306/longlian_oa_diff"
#   ./db/migrate.sh plan    # 预览将执行的 SQL，不改库
#   ./db/migrate.sh apply   # 执行同步

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ACTION="${1:-apply}"

# 检查 Atlas 是否可用（优先本地 CLI，否则用 Docker）
if command -v atlas >/dev/null 2>&1; then
  ATLAS="atlas"
elif command -v docker >/dev/null 2>&1; then
  ATLAS="docker run --rm -v ${SCRIPT_DIR}:/work -w /work --network=host \
    -e DB_URL -e DEV_DB_URL arigaio/atlas:1.2.3"
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

cd "$SCRIPT_DIR"

case "$ACTION" in
  apply)
    check_env
    echo "==> 同步数据库到 schema.sql 声明状态..."
    $ATLAS schema apply --env prod --auto-approve
    echo "==> 同步完成"
    ;;
  plan)
    check_env
    echo "==> 计划将执行的变更（不改动数据库）..."
    $ATLAS schema apply --env prod --dry-run
    ;;
  inspect)
    check_env
    $ATLAS schema inspect --env prod
    ;;
  update_schema)
    check_env
    $ATLAS schema inspect --env prod --format "{{ sql .}}" > schema.sql
    ;;
  *)
    echo "用法: $0 [apply|plan|inspect]" >&2
    exit 1
    ;;
esac
