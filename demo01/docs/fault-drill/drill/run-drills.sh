#!/usr/bin/env bash
# 实验任务 5.1 故障演练：不改动 src 下的源码，通过临时 classpath 覆盖故障版 Mapper XML。
# 运行前需已完成编译，并生成 target/runtime-classpath.txt。
set -u

BASE=$(cd "$(dirname "$0")/../../.." && pwd)
DRILL_DIR="$BASE/docs/fault-drill"
WORK="${TMPDIR:-/tmp}/mybatis-fault-drill"
LOGS="$DRILL_DIR/logs"
CP_FILE="$BASE/target/runtime-classpath.txt"
PROPS="$BASE/src/main/resources/db.properties"
DB_NAME="bank_fault_drill"

mkdir -p "$WORK/mapper" "$WORK/classes" "$LOGS"

if [[ ! -f "$CP_FILE" || ! -d "$BASE/target/classes" ]]; then
  echo "缺少 target/classes 或 runtime-classpath.txt，请先执行：" >&2
  echo "  ./apache-maven-3.9.6/bin/mvn -o -Dmaven.repo.local=./maven_localdb -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt" >&2
  exit 1
fi

DB_URL=$(sed -n 's/^jdbc\.url=//p' "$PROPS" | head -n1)
DB_USER=$(sed -n 's/^jdbc\.username=//p' "$PROPS" | head -n1)
DB_PASS=$(sed -n 's/^jdbc\.password=//p' "$PROPS" | head -n1)
HOST_PORT=${DB_URL#jdbc:mysql://}
HOST_PORT=${HOST_PORT%%/*}
if [[ "$HOST_PORT" == *:* ]]; then HOST=${HOST_PORT%%:*}; PORT=${HOST_PORT##*:}; else HOST=$HOST_PORT; PORT=3306; fi
QUERY=""
if [[ "$DB_URL" == *\?* ]]; then QUERY="?${DB_URL#*\?}"; fi
DRILL_URL="jdbc:mysql://$HOST_PORT/$DB_NAME$QUERY"

cat > "$WORK/db.properties" <<EOF
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=$DRILL_URL
jdbc.username=$DB_USER
jdbc.password=$DB_PASS
EOF

mysql_cmd() { MYSQL_PWD="$DB_PASS" mysql -h "$HOST" -P "$PORT" -u "$DB_USER" "$@"; }

recreate_db() {
  mysql_cmd -e "DROP DATABASE IF EXISTS $DB_NAME; CREATE DATABASE $DB_NAME DEFAULT CHARACTER SET utf8mb4" 2>/dev/null
  mysql_cmd "$DB_NAME" < "$BASE/sql/bank_init.sql" 2>/dev/null
}

javac -encoding UTF-8 -cp "$BASE/target/classes:$(cat "$CP_FILE")" -d "$WORK/classes" "$DRILL_DIR/drill/FaultDrill.java"

run_case() {
  local mode="$1" xml="$2" sed_expr="$3" out="$4" title="$5"
  rm -f "$WORK/mapper/"*.xml
  recreate_db
  sed "$sed_expr" "$BASE/src/main/resources/mapper/$xml" > "$WORK/mapper/$xml"
  {
    echo "# 演练：$title"
    echo "# 故障设置：$sed_expr"
    echo "# 故障文件：$xml"
    echo "# 命令：java -Dconsole.color=never -cp <tmp>:target/classes:<deps> FaultDrill $mode"
    echo
  } > "$LOGS/$out"
  java -Dconsole.color=never -cp "$WORK:$WORK/classes:$BASE/target/classes:$(cat "$CP_FILE")" FaultDrill "$mode" >> "$LOGS/$out" 2>&1
  local code=$?
  echo "" >> "$LOGS/$out"
  echo "退出码：$code" >> "$LOGS/$out"
  echo "已生成 ${LOGS}/${out}（退出码 ${code}）"
}

# 场景一：占位符参数名写错，触发 Parameter not found
run_case parameter BankAccountMapper.xml 's/#{accountNo}/#{accountno}/g' \
  01-parameter-binding.log "参数名写错：#{accountNo} -> #{accountno}"

# 场景二：resultMap 的 column 写错，查询不报错但映射结果为 null
run_case resultmap BankAccountMapper.xml 's/column="account_no"/column="account_no_typo"/' \
  02-resultmap-column.log "resultMap 列名写错：account_no -> account_no_typo"

# 场景三：去掉 useGeneratedKeys/keyProperty，主键回填失败
run_case generated-key CustomerMapper.xml 's/ useGeneratedKeys="true" keyProperty="customers.id"//' \
  03-generated-key.log "去掉 useGeneratedKeys/keyProperty：主键为 null"

mysql_cmd -e "DROP DATABASE IF EXISTS $DB_NAME" 2>/dev/null
echo "临时数据库 $DB_NAME 已删除"
