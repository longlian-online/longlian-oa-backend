#!/usr/bin/env bash
# 由 CNB 部署流水线在构建节点执行。
# 密钥来自密钥仓库 imports，不在本脚本里。
# 未设置 APP_TAG 时，从标签 vX.Y.Z 推出 X.Y.Z 与 X.Y.Z-migration。
set -Eeuo pipefail

if [ -n "${APP_TAG:-}" ]; then
  : "${MIGRATION_TAG:?MIGRATION_TAG is required}"
  case "$APP_TAG" in
    *[!A-Za-z0-9._-]*)
      echo "APP_TAG contains unsupported characters" >&2
      exit 1
      ;;
  esac
  case "$MIGRATION_TAG" in
    *[!A-Za-z0-9._-]*)
      echo "MIGRATION_TAG contains unsupported characters" >&2
      exit 1
      ;;
  esac
else
  version="${CNB_BRANCH#v}"
  printf '%s\n' "$version" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'
  APP_TAG=$version
  MIGRATION_TAG="${version}-migration"
fi

: "${DEPLOY_SSH_HOST:?DEPLOY_SSH_HOST is required}"
: "${DEPLOY_SSH_USER:?DEPLOY_SSH_USER is required}"
: "${DEPLOY_SSH_KEY:?DEPLOY_SSH_KEY is required}"
: "${DEPLOY_SSH_KNOWN_HOSTS:?DEPLOY_SSH_KNOWN_HOSTS is required}"
: "${DEPLOY_DIR:?DEPLOY_DIR is required}"
case "$DEPLOY_DIR" in
  *[!A-Za-z0-9._/-]*)
    echo "DEPLOY_DIR contains unsupported characters" >&2
    exit 1
    ;;
esac
umask 077
mkdir -p "$HOME/.ssh"

write_secret() {
  value="$1"
  dest="$2"
  case "$value" in
    *$'\n'*) printf '%s\n' "$value" > "$dest" ;;
    *) printf '%b\n' "$value" > "$dest" ;;
  esac
}

write_secret "$DEPLOY_SSH_KEY" "$HOME/.ssh/id"
write_secret "$DEPLOY_SSH_KNOWN_HOSTS" "$HOME/.ssh/known_hosts"
chmod 600 "$HOME/.ssh/id" "$HOME/.ssh/known_hosts"

remote="set -Eeuo pipefail; cd $(printf '%q' "$DEPLOY_DIR"); export APP_TAG=$(printf '%q' "$APP_TAG"); export MIGRATION_TAG=$(printf '%q' "$MIGRATION_TAG"); docker compose up -d --pull always"

exec ssh -i "$HOME/.ssh/id" \
  -p "${DEPLOY_SSH_PORT:-22}" \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile="$HOME/.ssh/known_hosts" \
  "${DEPLOY_SSH_USER}@${DEPLOY_SSH_HOST}" \
  "bash -lc $(printf '%q' "$remote")"
