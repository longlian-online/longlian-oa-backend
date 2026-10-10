#!/usr/bin/env sh
# 使用真实 MariaDB 和 Atlas 验证迁移入口，只创建并清理本次专用数据库。
# 需要 MYSQL_HOST / MYSQL_TCP_PORT / MYSQL_USER / MYSQL_PWD 和不含库名的 DB_SERVER_URL。
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
: "${DB_SERVER_URL:?需要 DB_SERVER_URL}"
: "${MYSQL_HOST:?需要 MYSQL_HOST}"
: "${MYSQL_TCP_PORT:?需要 MYSQL_TCP_PORT}"
: "${MYSQL_USER:?需要 MYSQL_USER}"
: "${MYSQL_PWD:?需要 MYSQL_PWD}"
export MYSQL_PWD
work=$(mktemp -d)
database="data_migration_test_$$"
shadow="${database}_atlas"
fresh="${database}_fresh"
production="${database}_prod"

sql() {
  mariadb --protocol=TCP --default-character-set=utf8mb4 \
    -h "$MYSQL_HOST" -P "$MYSQL_TCP_PORT" -u "$MYSQL_USER" -N -B "$@"
}
cleanup() {
  sql -e "DROP DATABASE IF EXISTS \`$database\`; DROP DATABASE IF EXISTS \`$shadow\`; DROP DATABASE IF EXISTS \`$fresh\`; DROP DATABASE IF EXISTS \`$production\`;"
  rm -rf "$work"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
sql -e "CREATE DATABASE \`$database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"
mkdir -p "$work/db/data-migrations" "$work/db/seed"
cp "$ROOT/migrate.sh" "$ROOT/atlas.hcl" "$ROOT/schema.sql" "$work/db/"
cp "$ROOT/seed/base_data.sql" "$work/db/seed/"
cp "$ROOT/data-migrations/"*.sql "$work/db/data-migrations/"
printf '\nCREATE TABLE migration_probe (id bigint NOT NULL AUTO_INCREMENT, value varchar(40) NOT NULL, PRIMARY KEY (id)) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci;\n' >> "$work/db/schema.sql"
sql "$database" < "$work/db/schema.sql"
sql "$database" -e "DROP TABLE data_migration; ALTER TABLE base_task DROP COLUMN icon, ADD COLUMN icon_name varchar(100) NULL, ADD COLUMN icon_file_id bigint NULL; INSERT INTO base_task (id, org_id, name, meta_schema, creator_id, icon_name, icon_file_id) VALUES (1,1,'a','[]',1,'Camera',NULL),(2,1,'b','[]',1,'',NULL),(3,1,'c','[]',1,NULL,9);"
# 引用 Atlas 将新增的列，只有结构同步完成后才能成功执行。
printf "INSERT INTO migration_probe (value) SELECT IF(icon IS NULL, 'schema-ready', 'unexpected') FROM base_task WHERE id=1;\n" > "$work/db/data-migrations/01_schema_ready.sql"

export DB_URL="${DB_SERVER_URL}/${database}"
export DEV_DB_URL="${DB_SERVER_URL}/${shadow}"
run() { sh "$work/db/migrate.sh" "$@"; }
assert_sql() {
  actual=$(sql "$database" -e "$1")
  if [ "$actual" != "$2" ]; then
    printf 'FAIL: %s\nexpected: %s\nactual: %s\n' "$1" "$2" "$actual" >&2
    exit 1
  fi
}

# 预览不执行数据文件、不创建记录表，也不删除旧字段。
run dev plan > "$work/plan.log"
assert_sql "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='data_migration'" 0
assert_sql "SELECT COUNT(*) FROM migration_probe" 0
case "$(cat "$work/plan.log")" in
  *01_schema_ready.sql*base_task_icon.sql*) ;;
  *) echo 'FAIL: plan 未展示待执行数据文件' >&2; exit 1 ;;
esac
printf 'PASS: plan does not execute or record data migrations\n'

run dev apply > "$work/first.log"
assert_sql "SELECT GROUP_CONCAT(filename ORDER BY filename) FROM data_migration" '01_schema_ready.sql,base_task_icon.sql'
assert_sql "SELECT value FROM migration_probe" schema-ready
assert_sql "SELECT icon FROM base_task WHERE id=1" Camera
assert_sql "SELECT COUNT(*) FROM base_task WHERE id IN (2,3) AND icon IS NULL" 2
assert_sql "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='base_task' AND COLUMN_NAME IN ('icon_name','icon_file_id')" 2
printf 'PASS: dev retains old columns; data files run after Atlas and preserve old values\n'

run dev apply > "$work/repeat.log"
assert_sql "SELECT COUNT(*) FROM data_migration WHERE executed_at IS NOT NULL" 2
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe" schema-ready
run dev plan > "$work/completed-plan.log"
case "$(cat "$work/completed-plan.log")" in
  *'数据迁移:'*) echo 'FAIL: plan 未跳过已记录文件' >&2; exit 1 ;;
esac
printf 'PASS: apply and plan skip successfully recorded filenames\n'

# 失败不能提交数据或记录，也不能执行后续文件；修正未成功文件后可以恢复。
printf "INSERT INTO migration_probe (value) VALUES ('failed');\nSELECT * FROM missing_migration_fixture;\n" > "$work/db/data-migrations/02_retry.sql"
printf "INSERT INTO migration_probe (value) VALUES ('later');\n" > "$work/db/data-migrations/03_later.sql"
if run dev apply > "$work/failure.log" 2>&1; then
  echo 'FAIL: 错误 SQL 未阻止 apply' >&2
  exit 1
fi
assert_sql "SELECT COUNT(*) FROM data_migration" 2
assert_sql "SELECT COUNT(*) FROM migration_probe WHERE value IN ('failed','later')" 0
printf "INSERT INTO migration_probe (value) VALUES ('recovered');\n" > "$work/db/data-migrations/02_retry.sql"
run dev apply > "$work/retry.log"
assert_sql "SELECT COUNT(*) FROM data_migration" 4
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe" 'schema-ready,recovered,later'
printf 'PASS: failed data and record roll back; recovery executes remaining files in order\n'

printf "INSERT INTO migration_probe (value) VALUES ('upper');\n" > "$work/db/data-migrations/04_Case.sql"
printf "INSERT INTO migration_probe (value) VALUES ('lower');\n" > "$work/db/data-migrations/04_case.sql"
run dev apply > "$work/case.log"
assert_sql "SELECT COUNT(*) FROM data_migration WHERE filename IN ('04_Case.sql','04_case.sql')" 2
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe WHERE value IN ('upper','lower')" 'upper,lower'
printf 'PASS: filenames are case-sensitive execution identities\n'

sql -e "CREATE DATABASE \`$fresh\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"
DB_URL="${DB_SERVER_URL}/${fresh}" sh "$ROOT/migrate.sh" dev apply > "$work/fresh.log"
actual=$(sql "$fresh" -e "SELECT filename FROM data_migration")
[ "$actual" = base_task_icon.sql ] || { echo 'FAIL: 新库未记录成功' >&2; exit 1; }
printf 'PASS: fresh database creates schema and records data migrations\n'

# 已有目标值不覆盖；生产环境同样保留旧字段，供同步后的数据复制读取。
sql -e "CREATE DATABASE \`$production\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"
sql "$production" < "$ROOT/schema.sql"
sql "$production" -e "ALTER TABLE base_task ADD COLUMN icon_name varchar(100) NULL, ADD COLUMN icon_file_id bigint NULL; INSERT INTO base_task (id, org_id, name, creator_id, icon, icon_name) VALUES (1,1,'a',1,'BadgeCheck','Camera'),(2,1,'b',1,NULL,'AArrowUp');"
DB_URL="${DB_SERVER_URL}/${production}" sh "$ROOT/migrate.sh" prod apply > "$work/prod.log"
actual=$(sql "$production" -e "SELECT GROUP_CONCAT(icon ORDER BY id) FROM base_task")
[ "$actual" = 'BadgeCheck,AArrowUp' ] || { echo 'FAIL: 生产旧值复制或目标值保留失败' >&2; exit 1; }
actual=$(sql "$production" -e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='base_task' AND COLUMN_NAME IN ('icon_name','icon_file_id')")
[ "$actual" = 2 ] || { echo 'FAIL: 生产环境自动删除了旧列' >&2; exit 1; }
actual=$(sql "$production" -e "SELECT filename FROM data_migration")
[ "$actual" = base_task_icon.sql ] || { echo 'FAIL: 生产迁移未记录成功' >&2; exit 1; }
printf 'PASS: production retains old columns and existing target values\n'
