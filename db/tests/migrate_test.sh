#!/usr/bin/env sh
# 使用真实 MariaDB 和 Atlas 验证通用迁移入口；只创建并清理本次专用数据库。
# 需要 MYSQL_HOST / MYSQL_TCP_PORT / MYSQL_USER / MYSQL_PWD 和 DB_SERVER_URL（maria://...，不含库名）。
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
: "${DB_SERVER_URL:?需要不含库名的 DB_SERVER_URL}"
: "${MYSQL_HOST:?需要 MYSQL_HOST}"
: "${MYSQL_TCP_PORT:?需要 MYSQL_TCP_PORT}"
: "${MYSQL_USER:?需要 MYSQL_USER}"
: "${MYSQL_PWD:?需要 MYSQL_PWD}"
export MYSQL_PWD
work=$(mktemp -d)
database="data_migration_test_$$"
shadow="${database}_atlas"

sql() {
  mariadb --protocol=TCP --default-character-set=utf8mb4 \
    -h "$MYSQL_HOST" -P "$MYSQL_TCP_PORT" -u "$MYSQL_USER" -N -B "$@"
}

cleanup() {
  sql -e "DROP DATABASE IF EXISTS \`$database\`; DROP DATABASE IF EXISTS \`$shadow\`;"
  rm -rf "$work"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
sql -e "CREATE DATABASE \`$database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"
mkdir -p "$work/db/data-migrations/before-schema" "$work/db/data-migrations/after-schema" "$work/db/seed"
cp "$ROOT/migrate.sh" "$ROOT/atlas.hcl" "$ROOT/schema.sql" "$work/db/"
cp "$ROOT/seed/base_data.sql" "$work/db/seed/"
cp "$ROOT/data-migrations/before-schema/"*.sql "$work/db/data-migrations/before-schema/"
printf '\nCREATE TABLE migration_probe (id bigint NOT NULL AUTO_INCREMENT, value varchar(40) NOT NULL, extra int NOT NULL DEFAULT 0, PRIMARY KEY (id)) CHARSET utf8mb4 COLLATE utf8mb4_unicode_520_ci;\n' >> "$work/db/schema.sql"
sql "$database" < "$work/db/schema.sql"
sql "$database" -e "DROP TABLE data_migration; ALTER TABLE base_task DROP COLUMN icon, ADD COLUMN icon_name varchar(100) NULL, ADD COLUMN icon_file_id bigint NULL; INSERT INTO base_task (id, org_id, name, meta_schema, creator_id, icon_name, icon_file_id) VALUES (1,1,'a','[]',1,'Camera',NULL),(2,1,'b','[]',1,'',NULL),(3,1,'c','[]',1,NULL,9);"
printf "INSERT INTO migration_probe (value) VALUES ('before');\n" > "$work/db/data-migrations/before-schema/01_probe_before.sql"
printf "INSERT INTO migration_probe (value) VALUES ('after');\n" > "$work/db/data-migrations/after-schema/01_probe_after.sql"

export DB_URL="${DB_SERVER_URL}/${database}"
export DEV_DB_URL="${DB_SERVER_URL}/${shadow}"
run() {
  sh "$work/db/migrate.sh" "$@"
}
assert_sql() {
  actual=$(sql "$database" -e "$1")
  if [ "$actual" != "$2" ]; then
    printf 'FAIL: %s\nexpected: %s\nactual: %s\n' "$1" "$2" "$actual" >&2
    exit 1
  fi
}

# plan 展示待执行文件，但不创建记录表、不执行数据 SQL。
run dev plan > "$work/plan.log"
assert_sql "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='data_migration'" 0
assert_sql "SELECT COUNT(*) FROM migration_probe" 0
case "$(cat "$work/plan.log")" in
  *01_probe_before.sql*01_probe_after.sql*) ;;
  *) echo 'FAIL: plan 未展示两个阶段的待执行文件' >&2; exit 1 ;;
esac
printf 'PASS: plan does not execute or record data migrations\n'

# 第一次执行同时覆盖记录表引导、旧数据保留和后置阶段。
run dev apply > "$work/first.log"
assert_sql "SELECT GROUP_CONCAT(filename ORDER BY filename) FROM data_migration" '01_probe_after.sql,01_probe_before.sql,base_task_icon.sql'
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe" 'before,after'
assert_sql "SELECT icon FROM base_task WHERE id=1" Camera
assert_sql "SELECT COUNT(*) FROM base_task WHERE id IN (2,3) AND icon IS NULL" 2
assert_sql "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='base_task' AND COLUMN_NAME IN ('icon_name','icon_file_id')" 0
run dev apply > "$work/repeat.log"
assert_sql "SELECT COUNT(*) FROM data_migration WHERE executed_at IS NOT NULL" 3
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe" 'before,after'
printf 'PASS: first apply preserves data; repeated apply skips recorded filenames\n'

# 错误不能提交纯 DML、写成功记录或继续执行后续文件。
printf "INSERT INTO migration_probe (value) VALUES ('failed');\nSELECT * FROM missing_migration_fixture;\n" > "$work/db/data-migrations/after-schema/02_retry.sql"
printf "INSERT INTO migration_probe (value) VALUES ('later');\n" > "$work/db/data-migrations/after-schema/03_later.sql"
if run dev apply > "$work/failure.log" 2>&1; then
  echo 'FAIL: 错误 SQL 未阻止 apply' >&2
  exit 1
fi
assert_sql "SELECT COUNT(*) FROM data_migration" 3
assert_sql "SELECT COUNT(*) FROM migration_probe WHERE value IN ('failed','later')" 0
printf "INSERT INTO migration_probe (value) VALUES ('recovered');\n" > "$work/db/data-migrations/after-schema/02_retry.sql"
run dev apply > "$work/retry.log"
assert_sql "SELECT COUNT(*) FROM data_migration" 5
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe" 'before,after,recovered,later'
printf 'PASS: failed DML rolls back, is not recorded, and can recover\n'

# MariaDB DDL 隐式提交后失败，必须可重入；成功后才写记录。
sql "$database" -e "ALTER TABLE migration_probe DROP COLUMN extra;"
printf "ALTER TABLE migration_probe ADD COLUMN IF NOT EXISTS extra int NOT NULL DEFAULT 0;\nSELECT * FROM missing_migration_fixture;\n" > "$work/db/data-migrations/before-schema/04_ddl_retry.sql"
if run dev apply > "$work/ddl-failure.log" 2>&1; then
  echo 'FAIL: DDL 后的错误 SQL 未阻止 apply' >&2
  exit 1
fi
assert_sql "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='migration_probe' AND COLUMN_NAME='extra'" 1
assert_sql "SELECT COUNT(*) FROM data_migration WHERE filename='04_ddl_retry.sql'" 0
printf "ALTER TABLE migration_probe ADD COLUMN IF NOT EXISTS extra int NOT NULL DEFAULT 0;\n" > "$work/db/data-migrations/before-schema/04_ddl_retry.sql"
run dev apply > "$work/ddl-retry.log"
assert_sql "SELECT COUNT(*) FROM data_migration WHERE filename='04_ddl_retry.sql'" 1
printf 'PASS: interrupted DDL resumes without a false success record\n'

# 文件名是全局标识，两阶段同名必须在执行任何 SQL 前拒绝。
printf "INSERT INTO migration_probe (value) VALUES ('duplicate');\n" > "$work/db/data-migrations/before-schema/05_duplicate.sql"
printf "INSERT INTO migration_probe (value) VALUES ('duplicate');\n" > "$work/db/data-migrations/after-schema/05_duplicate.sql"
if run dev apply > "$work/duplicate.log" 2>&1; then
  echo 'FAIL: 两阶段同名文件未被拒绝' >&2
  exit 1
fi
assert_sql "SELECT COUNT(*) FROM migration_probe WHERE value='duplicate'" 0
rm "$work/db/data-migrations/before-schema/05_duplicate.sql" "$work/db/data-migrations/after-schema/05_duplicate.sql"
printf 'PASS: duplicate filenames are rejected before execution\n'

# 新库也能完成前置空操作、目标结构建立和记录写入。
fresh="${database}_fresh"
sql -e "CREATE DATABASE \`$fresh\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"
# 清理函数始终包含本次新建的库，即使验证失败。
cleanup() {
  sql -e "DROP DATABASE IF EXISTS \`$database\`; DROP DATABASE IF EXISTS \`$shadow\`; DROP DATABASE IF EXISTS \`$fresh\`;"
  rm -rf "$work"
}

# 文件标识区分大小写，不能被数据库默认的不区分大小写排序规则合并。
printf "INSERT INTO migration_probe (value) VALUES ('upper');\n" > "$work/db/data-migrations/after-schema/06_Case.sql"
printf "INSERT INTO migration_probe (value) VALUES ('lower');\n" > "$work/db/data-migrations/after-schema/06_case.sql"
run dev apply > "$work/case.log"
assert_sql "SELECT COUNT(*) FROM data_migration WHERE filename IN ('06_Case.sql','06_case.sql')" 2
assert_sql "SELECT GROUP_CONCAT(value ORDER BY id) FROM migration_probe WHERE value IN ('upper','lower')" 'upper,lower'
printf 'PASS: filenames are case-sensitive execution identities\n'
DB_URL="${DB_SERVER_URL}/${fresh}" sh "$ROOT/migrate.sh" dev apply > "$work/fresh.log"
actual=$(sql "$fresh" -e "SELECT filename FROM data_migration")
[ "$actual" = base_task_icon.sql ] || { echo 'FAIL: 新库未记录迁移' >&2; exit 1; }
printf 'PASS: fresh database bootstraps schema and migration records\n'

# 生产结构同步必须保留来源列，不能由自动迁移绕过删除保护。
production="${database}_prod"
cleanup() {
  sql -e "DROP DATABASE IF EXISTS \`$database\`; DROP DATABASE IF EXISTS \`$shadow\`; DROP DATABASE IF EXISTS \`$fresh\`; DROP DATABASE IF EXISTS \`$production\`;"
  rm -rf "$work"
}
sql -e "CREATE DATABASE \`$production\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"
sql "$production" < "$ROOT/schema.sql"
sql "$production" -e "ALTER TABLE base_task DROP COLUMN icon, ADD COLUMN icon_name varchar(100) NULL, ADD COLUMN icon_file_id bigint NULL; INSERT INTO base_task (id, org_id, name, creator_id, icon_name) VALUES (1,1,'a',1,'Camera');"
DB_URL="${DB_SERVER_URL}/${production}" sh "$ROOT/migrate.sh" prod apply > "$work/prod.log"
actual=$(sql "$production" -e "SELECT icon FROM base_task WHERE id=1")
[ "$actual" = Camera ] || { echo 'FAIL: 生产旧值未保留' >&2; exit 1; }
actual=$(sql "$production" -e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='base_task' AND COLUMN_NAME IN ('icon_name','icon_file_id')")
[ "$actual" = 2 ] || { echo 'FAIL: 自动迁移绕过生产旧列删除保护' >&2; exit 1; }
actual=$(sql "$production" -e "SELECT filename FROM data_migration")
[ "$actual" = base_task_icon.sql ] || { echo 'FAIL: 生产迁移未记录成功' >&2; exit 1; }
printf 'PASS: production apply preserves values and retains protected old columns\n'
