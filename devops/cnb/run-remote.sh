#!/usr/bin/env bash
# 连接目标机器并执行 DEPLOY_SCRIPT_PATH。怎么部署由那台机器上的脚本决定。
set -Eeuo pipefail

if [ -z "${APP_TAG:-}" ]; then
  version="${CNB_BRANCH#v}"
  printf '%s\n' "$version" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'
  APP_TAG="$version"
  MIGRATION_TAG="${version}-migration"
fi

: "${MIGRATION_TAG:?MIGRATION_TAG is required}"
: "${DEPLOY_SSH_HOST:?DEPLOY_SSH_HOST is required}"
: "${DEPLOY_SSH_USER:?DEPLOY_SSH_USER is required}"
: "${DEPLOY_SSH_KEY:?DEPLOY_SSH_KEY is required}"
: "${DEPLOY_SSH_KNOWN_HOSTS:?DEPLOY_SSH_KNOWN_HOSTS is required}"
: "${DEPLOY_SCRIPT_PATH:?DEPLOY_SCRIPT_PATH is required}"

case "$APP_TAG" in
  *[!A-Za-z0-9._-]*) echo "APP_TAG contains unsupported characters" >&2; exit 1 ;;
esac
case "$MIGRATION_TAG" in
  *[!A-Za-z0-9._-]*) echo "MIGRATION_TAG contains unsupported characters" >&2; exit 1 ;;
esac
case "$DEPLOY_SCRIPT_PATH" in
  *[!A-Za-z0-9._/-]*) echo "DEPLOY_SCRIPT_PATH contains unsupported characters" >&2; exit 1 ;;
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

remote="export APP_TAG=$(printf '%q' "$APP_TAG"); export MIGRATION_TAG=$(printf '%q' "$MIGRATION_TAG"); $(printf '%q' "$DEPLOY_SCRIPT_PATH")"

exec ssh -i "$HOME/.ssh/id" \
  -p "${DEPLOY_SSH_PORT:-22}" \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile="$HOME/.ssh/known_hosts" \
  "${DEPLOY_SSH_USER}@${DEPLOY_SSH_HOST}" \
  "bash -lc $(printf '%q' "$remote")"
