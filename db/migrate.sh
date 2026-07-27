#!/usr/bin/env sh
# 数据库同步脚本（声明式）
# 用法: ./migrate.sh <dev|prod> [apply|plan|inspect]
#
# 环境变量:
#   DB_URL       - 目标数据库连接地址 (必须)
#   DEV_DB_URL   - Atlas 暂存库地址 (必须，会被反复清空，须为专用空库)
#
# 示例:
#   export DB_URL="postgresql://postgres:pass@localhost:5432/longlian_oa"
#   export DEV_DB_URL="postgresql://postgres:pass@localhost:5432/longlian_oa_diff"
#   ./db/migrate.sh dev plan    # 预览开发环境变更
#   ./db/migrate.sh dev apply   # 开发环境完整同步，允许删除废弃对象
#   ./db/migrate.sh prod plan   # 生产环境变更预览
#   ./db/migrate.sh prod apply  # 生产环境同步，不自动删除对象

set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENVIRONMENT="${1:-}"
ACTION="${2:-apply}"

# 检查 Atlas 是否可用（优先本地 CLI，否则用 Docker）
if command -v atlas >/dev/null 2>&1; then
  run_atlas() {
    atlas "$@"
  }
elif command -v docker >/dev/null 2>&1; then
  run_atlas() {
    docker run --rm -v "${SCRIPT_DIR}:/work" -w /work --network=host \
      -e DB_URL -e DEV_DB_URL arigaio/atlas:1.2.3 "$@"
  }
else
  echo "错误: 需要安装 atlas CLI 或 docker" >&2
  exit 1
fi

check_env() {
  if [ -z "${DB_URL:-}" ]; then
    echo "错误: 需要设置 DB_URL 环境变量" >&2
    exit 1
  fi
  if [ -z "${DEV_DB_URL:-}" ]; then
    echo "错误: 需要设置 DEV_DB_URL 环境变量" >&2
    exit 1
  fi
}

usage() {
  echo "用法: $0 <dev|prod> [apply|plan|inspect]" >&2
}

case "$ENVIRONMENT" in
  dev|prod)
    ;;
  *)
    usage
    exit 1
    ;;
esac

case "$ACTION" in
  apply|plan|inspect)
    ;;
  *)
    usage
    exit 1
    ;;
esac

cd "$SCRIPT_DIR"

case "$ACTION" in
  apply)
    check_env
    echo "==> [${ENVIRONMENT}] 同步数据库到 schema.sql 声明状态..."
    run_atlas schema apply --env "$ENVIRONMENT" --auto-approve
    echo "==> [${ENVIRONMENT}] 同步完成"
    ;;
  plan)
    check_env
    echo "==> [${ENVIRONMENT}] 计划将执行的变更（不改动数据库）..."
    run_atlas schema apply --env "$ENVIRONMENT" --dry-run
    ;;
  inspect)
    check_env
    run_atlas schema inspect --env "$ENVIRONMENT"
    ;;
esac
