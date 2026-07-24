#!/usr/bin/env sh
# CI 数据库迁移脚本
# 用法: ./migrate.sh [apply|lint|hash]
#
# 环境变量:
#   DB_URL       - 目标数据库连接地址 (必须)
#   DEV_DB_URL   - 开发数据库地址，lint 时需要 (可选)
#
# 示例:
#   DB_URL="mysql://root:pass@db:3306/mydb" ./migrate.sh apply
#   DB_URL="mysql://root:pass@db:3306/mydb" DEV_DB_URL="mysql://root:pass@db:3306/dev" ./migrate.sh lint

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

case "$ACTION" in
  apply)
    if [ -z "$DB_URL" ]; then
      echo "错误: 需要设置 DB_URL 环境变量" >&2
      exit 1
    fi
    echo "==> 应用数据库迁移..."
    $ATLAS migrate apply --env local --url "$DB_URL"
    echo "==> 迁移完成"
    ;;
  lint)
    echo "==> 检查迁移文件..."
    $ATLAS migrate lint --env local --latest 1
    echo "==> 检查通过"
    ;;
  hash)
    echo "==> 更新 atlas.sum..."
    $ATLAS migrate hash --env local
    echo "==> 完成"
    ;;
  status)
    if [ -z "$DB_URL" ]; then
      echo "错误: 需要设置 DB_URL 环境变量" >&2
      exit 1
    fi
    $ATLAS migrate status --env local --url "$DB_URL"
    ;;
  *)
    echo "用法: $0 [apply|lint|hash|status]" >&2
    exit 1
    ;;
esac
