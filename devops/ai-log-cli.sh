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

service_json=$(printf '"%s"' "$service" | sed 's/\\/\\\\/g; s/"/\\"/g')
environment_json=$(printf '"%s"' "$environment" | sed 's/\\/\\\\/g; s/"/\\"/g')
since_json=$(printf '"%s"' "$since" | sed 's/\\/\\\\/g; s/"/\\"/g')
keyword_json=null
trace_id_json=null
if [ -n "$keyword" ]; then
  keyword_json=$(printf '"%s"' "$keyword" | sed 's/\\/\\\\/g; s/"/\\"/g')
fi
if [ -n "$trace_id" ]; then
  trace_id_json=$(printf '"%s"' "$trace_id" | sed 's/\\/\\\\/g; s/"/\\"/g')
fi

payload=$(printf '{"service":%s,"environment":%s,"since":%s,"keyword":%s,"traceId":%s,"limit":%s}' \
  "$service_json" "$environment_json" "$since_json" "$keyword_json" "$trace_id_json" "$limit")

curl -fsS --fail-with-body -X POST "$gateway_url" \
  -H "Authorization: Bearer $token" \
  -H "Content-Type: application/json" \
  --data "$payload"
