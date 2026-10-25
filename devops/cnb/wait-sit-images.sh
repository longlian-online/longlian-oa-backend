#!/usr/bin/env bash
# GitHub 构建和 CNB 同步并行。sit 推送流水线要等到本次提交的镜像
# 出现在 CNB 制品库后再部署，否则会拉到上一版。
set -Eeuo pipefail

: "${CNB_COMMIT:?CNB_COMMIT is required}"
: "${CNB_TOKEN:?CNB_TOKEN is required}"

registry=https://docker.cnb.cool
repo=longlian.online/longlian-oa-backend
token=
deadline=$((SECONDS + 90 * 60))

basic_auth() {
  printf 'cnb:%s' "$CNB_TOKEN" | base64 | tr -d '\n'
}

ensure_token() {
  local headers realm service body auth
  if [ -n "$token" ]; then
    return 0
  fi
  headers=$(wget -S -O /dev/null "$registry/v2/" 2>&1 || true)
  realm=$(printf '%s\n' "$headers" | sed -n 's/.*realm="\([^"]*\)".*/\1/p' | head -1)
  service=$(printf '%s\n' "$headers" | sed -n 's/.*service="\([^"]*\)".*/\1/p' | head -1)
  if [ -z "$realm" ] || [ -z "$service" ]; then
    echo "registry did not return a bearer challenge" >&2
    return 1
  fi
  auth=$(basic_auth)
  body=$(wget -qO- --header="Authorization: Basic ${auth}" \
    "${realm}?service=${service}&scope=repository:${repo}:pull") || return 1
  token=$(printf '%s' "$body" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
  if [ -z "$token" ]; then
    token=$(printf '%s' "$body" | sed -n 's/.*"access_token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
  fi
  [ -n "$token" ]
}

config_digest() {
  printf '%s' "$1" | sed -n 's/.*"config"[[:space:]]*:[[:space:]]*{[^}]*"digest"[[:space:]]*:[[:space:]]*"\(sha256:[a-f0-9]\{64\}\)".*/\1/p' | head -1
}

image_revision() {
  printf '%s' "$1" | sed -n 's/.*"org.opencontainers.image.revision"[[:space:]]*:[[:space:]]*"\([0-9a-f]\{40\}\)".*/\1/p' | head -1
}

revision_of() {
  local tag="$1" manifest digest blob revision
  ensure_token || return 1
  manifest=$(wget -qO- \
    --header="Authorization: Bearer ${token}" \
    --header="Accept: application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json" \
    "$registry/v2/${repo}/manifests/${tag}") || return 1
  digest=$(config_digest "$manifest")
  [ -n "$digest" ] || return 1
  blob=$(wget -qO- \
    --header="Authorization: Bearer ${token}" \
    "$registry/v2/${repo}/blobs/${digest}") || return 1
  revision=$(image_revision "$blob")
  [ -n "$revision" ] || return 1
  printf '%s' "$revision"
}

while true; do
  ready=0
  for tag in sit sit-migration; do
    if revision=$(revision_of "$tag" 2>/dev/null); then
      echo "image ${tag} revision=${revision}"
      if [ "$revision" = "$CNB_COMMIT" ]; then
        ready=$((ready + 1))
      fi
    else
      echo "image ${tag} is not ready"
    fi
  done
  if [ "$ready" -eq 2 ]; then
    exit 0
  fi
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "timed out waiting for sit images of ${CNB_COMMIT}" >&2
    exit 1
  fi
  sleep 30
done
