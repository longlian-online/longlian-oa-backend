#!/usr/bin/env sh
set -eu

gateway_url="${AI_LOG_GATEWAY_URL:-http://127.0.0.1:18080/api/v1/logs/search}"
token="${AI_LOG_QUERY_TOKEN:-}"
service=""
environment=""
since="15m"
keyword=""
trace_id=""
limit="100"

usage() {
  echo "Usage: $0 --token TOKEN --service SERVICE --environment ENV [--since 15m] [--keyword TEXT] [--trace-id ID] [--limit N]"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --token) token=$2; shift 2 ;;
    --service) service=$2; shift 2 ;;
    --environment) environment=$2; shift 2 ;;
    --since) since=$2; shift 2 ;;
    --keyword) keyword=$2; shift 2 ;;
    --trace-id) trace_id=$2; shift 2 ;;
    --limit) limit=$2; shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [ -z "$token" ] || [ -z "$service" ] || [ -z "$environment" ]; then
  usage >&2
  exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to encode the JSON request safely" >&2
  exit 2
fi

if ! payload=$(jq -n \
  --arg service "$service" \
  --arg environment "$environment" \
  --arg since "$since" \
  --arg keyword "$keyword" \
  --arg trace_id "$trace_id" \
  --arg limit "$limit" \
  '{service: $service, environment: $environment, since: $since,
    keyword: (if $keyword == "" then null else $keyword end),
    traceId: (if $trace_id == "" then null else $trace_id end),
    limit: ($limit | tonumber)}'); then
  echo "limit must be a number" >&2
  exit 2
fi

curl -fsS --fail-with-body -X POST "$gateway_url" \
  -H "Authorization: Bearer $token" \
  -H "Content-Type: application/json" \
  --data "$payload"
