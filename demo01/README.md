# 银行存取款控制台

普通 Java 21 + Maven + MyBatis/MyBatis-Plus + MySQL 项目。一个客户可持有多个账户，支持客户管理、开户、存款、取款、余额和分页流水查询。

## 首次启动

1. 启动 MySQL（需要支持并执行 CHECK 约束的 MySQL 8.0.16+；已用环境为 8.4）。
2. 核对 `src/main/resources/db.properties` 中的数据库 URL、用户名和密码。沿用现有连接，不需要创建新的业务数据库。
3. 在 URL 指向的数据库执行 `sql/bank_init.sql`。例如：

   ```sh
   mysql -h localhost -u root -p mybatis_demo < sql/bank_init.sql
   ```

   脚本只创建三个 `bank_` 表，不删除旧表，不清空数据，也不会预置账户资金。可以重复执行；程序启动时会检查关键字段、主键、唯一键、外键和金额检查约束是否存在。结构不匹配时停止启动，需要人工核对结构，不会自动迁移。
4. 在 VS Code 打开 `src/main/java/org/example/Main.java`，点击 `main` 上方的 **Run** 或 **Debug**，无需程序参数。建议在 `BankService.changeBalance` 中的 `selectForUpdate`、`updateBalance`、流水 `insert` 处设置断点。观察锁时请勿同时长时间暂停多个事务。

命令行启动（macOS/Linux，在项目根目录）：

```sh
./apache-maven-3.9.6/bin/mvn -Dmaven.repo.local=maven_localdb -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "target/classes:$(cat target/runtime-classpath.txt)" org.example.Main
```

生成的是普通 JAR，不是带全部依赖的可执行 JAR，使用上述类路径启动。`org.example.DbConnectionTest` 可单独检查连接。

## 菜单与操作示例

- `1` 新增客户；`2` 修改姓名/电话，留空表示保持原字段。
- `3` 通过姓名、电话组合模糊查询；`4` 姓名优先精确查询，否则电话查询，否则全部。
- `5` 通过 MyBatis-Plus 条件构造器查询并分页，页码从 1 开始。
- `6` 批量新增客户；`7` 批量删除没有账户的客户，编号使用英文逗号分隔。只要一位客户已有账户，整批删除失败。
- `8` 开户：输入已有客户编号，或者留空登记新客户。系统显示自动生成的账号，初始余额为 `0.00`。
- `9` 存款；`10` 取款；`11` 查询账户及余额；`12` 分页查询流水；`13` 查询某客户的全部账户。
- `0` 退出；输入结束也会退出。启动入口会释放连接池。

完整体验：选择 `8` 开户并保存显示的账号，选择 `9` 存入 `100.50`，选择 `10` 取出 `20.25`，选择 `11` 应看到余额 `80.25`，选择 `12` 应有两条流水。开户本身不产生资金流水。

## 控制台输出约定

- 青色边框（`┌─ 结果标题 · 共 N 条`）内是**查询结果**，逐条编号；没有数据时显示 `暂无记录`。
- 灰色 `[日志] ...` 行是 **SQL 等执行过程**，边界清晰可忽略；根日志级别为 `INFO`，只有 Mapper 层保持 `DEBUG` 输出 SQL。
- `✔` 绿色表示成功，`✘` 红色表示失败，`!` 黄色表示菜单/输入提示，菜单本身用灰色弱化。
- 颜色自动检测；重定向到文件或终端不支持时可用 `-Dconsole.color=never` 关闭，`-Dconsole.color=always` 强制开启；`-Dconsole.width=120` 可调整结果框宽度。

## 数据和事务约定

- `Customer` 保留原有编号、姓名、电话，姓名和电话可以重复。客户、账户、流水分别对应 `bank_customer`、`bank_account`、`bank_transaction`。
- 账号为 `BA` 加 UUID 的 32 位十六进制字符串，数据库保证唯一。
- 金额为 `BigDecimal` / `DECIMAL(18,2)`，存取金额必须大于零且不能丢失小数精度；最大余额 `9999999999999999.99`。`1.2300` 可以精确规范为 `1.23`，`1.231` 被拒绝。
- 账户行锁、余额更新和流水插入使用同一个手动事务。余额不足、余额溢出或数据库写入异常均回滚，失败操作不产生资金流水。
- 不提供直接设置余额、删除账户或删除流水的菜单；客户外键禁止删除已有账户的客户。
- 原有 MyBatis 工厂、分页插件、日志配置和数据库连接检查程序保留；运行过程通过共享工厂取得独立会话。
- Log4j 的 `Preparing` 显示带 `?` 的 SQL，`Parameters` 按顺序显示实参及类型，`Total` 显示该 SQL 读取的结果行数。分页 COUNT 查询的 `Total: 1` 表示一行统计结果，业务记录总数查看 `Page.getTotal()`。
- 客户动态 SQL 演示 `if/where`、`set`、`foreach`、`choose/when/otherwise`。自定义 SQL 多参数显式使用 `@Param`；XML 位于 `mapper/`，因此配置中同时显式加载 XML 并扫描接口包。

## 测试

```sh
./apache-maven-3.9.6/bin/mvn -o -Dmaven.repo.local=maven_localdb clean verify
```

首次本地缓存不完整时去掉 `-o` 下载依赖。测试要求 MySQL 可连接，且配置的用户能够创建/删除临时数据库、创建表和测试触发器；权限不足时测试明确失败，不会退回业务数据库运行。

每个测试类创建唯一的 `bank_test_` 数据库，结束后删除；清理逻辑只针对测试自行创建的库，不清空业务库。涵盖 Mapper、参数边界、流水失败回滚、批量原子性、并发扣款、控制台和启动检查。`MainSmokeTest` 启动真正的 `Main` 子进程，完整演示记录写入 `target/bank-main-demo.log`。

## 改造范围与限制

已移除员工、部门、学生课程、商城、订单、身份证和旧用户示例的可见源码、映射、SQL、入口与测试；未删除数据库旧表。所有以 `.` 开头的文件和目录保持不动。

这是无登录的本地教学项目，未实现转账、利息、贷款和销户，不连接真实银行。未进行大数据量性能测试。依赖版本沿用原项目；本次没有引入新的业务框架。

实现与测试由 OpenAI Codex 协助完成，未复制外部第三方业务代码；如用于课程提交，请按课程要求保留或调整来源说明。
