package com.example.bank;

import com.example.service.BankService;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** 每个测试类独占随机数据库，任何清理只针对本类创建的库。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BankTestSupport {
    protected SqlSessionFactory factory;
    protected BankService bank;
    protected Properties properties;
    private Connection admin;
    private String schema;
    private boolean created;

    @BeforeAll
    void createDatabase() throws Exception {
        properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) throw new IllegalStateException("db.properties missing");
            properties.load(in);
        }
        String url = properties.getProperty("jdbc.url");
        int slash = url.indexOf('/', "jdbc:mysql://".length());
        if (slash < 0) throw new IllegalArgumentException("Tests require jdbc:mysql://host/database URL");
        int question = url.indexOf('?', slash);
        String suffix = question < 0 ? "" : url.substring(question);
        String serverUrl = url.substring(0, slash + 1) + suffix;
        schema = "bank_test_" + UUID.randomUUID().toString().replace("-", "");
        try {
            admin = DriverManager.getConnection(serverUrl, properties.getProperty("jdbc.username"), properties.getProperty("jdbc.password"));
            try (Statement st = admin.createStatement()) { st.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4"); }
            created = true;
            admin.setCatalog(schema);
            ScriptRunner runner = new ScriptRunner(admin);
            runner.setLogWriter(null); runner.setStopOnError(true);
            try (Reader script = Files.newBufferedReader(Path.of("sql/bank_init.sql"))) { runner.runScript(script); }
            // ScriptRunner 将连接设为手动提交；管理连接的后续清理必须立即提交，避免锁住业务测试。
            admin.setAutoCommit(true);
            properties.setProperty("jdbc.url", url.substring(0, slash + 1) + schema + suffix);
            factory = MyBatisUtil.createSqlSessionFactory(properties);
            bank = new BankService(factory);
        } catch (Exception | Error e) {
            closeDatabase();
            throw e;
        }
    }

    @BeforeEach
    void clearOwnedTables() throws Exception {
        if (!created || !schema.startsWith("bank_test_")) throw new IllegalStateException("Not a test-owned database");
        execute("DELETE FROM bank_transaction");
        execute("DELETE FROM bank_account");
        execute("DELETE FROM bank_customer");
    }

    protected void execute(String sql) throws SQLException {
        try (Statement st = admin.createStatement()) { st.execute(sql); }
    }

    protected long count(String table) throws SQLException {
        if (!Set.of("bank_customer", "bank_account", "bank_transaction").contains(table)) throw new IllegalArgumentException();
        try (Statement st = admin.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next(); return rs.getLong(1);
        }
    }

    @AfterAll
    void closeDatabase() throws SQLException {
        try {
            if (factory != null) ((PooledDataSource) factory.getConfiguration().getEnvironment().getDataSource()).forceCloseAll();
        } finally {
            if (admin != null) {
                try {
                    if (created) {
                        try (Statement st = admin.createStatement()) { st.execute("DROP DATABASE `" + schema + "`"); }
                        created = false;
                    }
                } finally { admin.close(); }
            }
        }
    }
}
