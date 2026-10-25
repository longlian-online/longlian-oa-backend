#!/usr/bin/env bash
# 由 CNB tag_deploy.production 在构建节点执行。
# 密钥来自密钥仓库 imports，不在本脚本里。
set -Eeuo pipefail

version="${CNB_BRANCH#v}"
printf '%s\n' "$version" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'

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

remote="set -Eeuo pipefail; cd $(printf '%q' "$DEPLOY_DIR"); export APP_TAG=$(printf '%q' "$version"); export MIGRATION_TAG=$(printf '%q' "${version}-migration"); docker compose up -d --pull always"

exec ssh -i "$HOME/.ssh/id" \
  -p "${DEPLOY_SSH_PORT:-22}" \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile="$HOME/.ssh/known_hosts" \
  "${DEPLOY_SSH_USER}@${DEPLOY_SSH_HOST}" \
  "bash -lc $(printf '%q' "$remote")"
