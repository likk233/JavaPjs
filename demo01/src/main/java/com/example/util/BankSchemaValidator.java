package com.example.util;

import org.apache.ibatis.session.SqlSessionFactory;
import java.sql.*;
import java.util.*;

/** 只读检查表结构，防止 IF NOT EXISTS 掩盖不兼容结构。 */
public final class BankSchemaValidator {
    private BankSchemaValidator() {}
    private record Column(String type, int size, int scale) {}
    private static Column c(String type, int size) { return new Column(type, size, 0); }
    private static final Column MONEY = new Column("DECIMAL", 18, 2);

    public static void validate(SqlSessionFactory factory) {
        try (var session = factory.openSession()) {
            Connection connection = session.getConnection();
            check(connection, "bank_customer", Map.of("id", c("INT", 10), "name", c("VARCHAR", 50), "phone", c("VARCHAR", 30)));
            check(connection, "bank_account", Map.of("id", c("BIGINT", 19), "account_no", c("VARCHAR", 34),
                    "customer_id", c("INT", 10), "balance", MONEY, "created_at", c("DATETIME", 0)));
            check(connection, "bank_transaction", Map.of("id", c("BIGINT", 19), "account_id", c("BIGINT", 19),
                    "type", c("VARCHAR", 8), "amount", MONEY, "balance_before", MONEY,
                    "balance_after", MONEY, "created_at", c("DATETIME", 0)));
            boolean unique;
            try (ResultSet rs = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, "bank_account", true, false)) {
                Map<String, List<String>> indexes = new HashMap<>();
                while (rs.next()) indexes.computeIfAbsent(rs.getString("INDEX_NAME"), key -> new ArrayList<>()).add(rs.getString("COLUMN_NAME"));
                unique = indexes.values().stream().anyMatch(columns -> columns.equals(List.of("account_no")));
            }
            if (!unique) throw new IllegalStateException("bank_account 缺少账号唯一约束");
            foreignKey(connection, "bank_account", "customer_id", "bank_customer");
            foreignKey(connection, "bank_transaction", "account_id", "bank_account");
            try (PreparedStatement st = connection.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'CHECK' AND ENFORCED = 'YES'
                    AND ((TABLE_NAME = 'bank_account' AND CONSTRAINT_NAME = 'chk_bank_account_balance')
                    OR (TABLE_NAME = 'bank_transaction' AND CONSTRAINT_NAME = 'chk_bank_transaction'))
                    """)) {
                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) != 2) throw new IllegalStateException("银行表缺少已启用的金额检查约束");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("银行表检查失败，请先执行 sql/bank_init.sql 并检查数据库配置", e);
        }
    }

    private static void check(Connection connection, String table, Map<String, Column> expected) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        Set<String> actual = new HashSet<>();
        try (ResultSet rs = meta.getColumns(connection.getCatalog(), null, table, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME").toUpperCase(Locale.ROOT);
                if (type.equals("INTEGER")) type = "INT";
                Column wanted = expected.get(name);
                if (wanted == null || !type.equals(wanted.type()) || rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls
                        || (!type.equals("DATETIME") && rs.getInt("COLUMN_SIZE") != wanted.size())
                        || (type.equals("DECIMAL") && rs.getInt("DECIMAL_DIGITS") != wanted.scale()))
                    throw new IllegalStateException(table + "." + name + " 的结构与银行初始化脚本不兼容");
                if (name.equals("id") && !"YES".equals(rs.getString("IS_AUTOINCREMENT")))
                    throw new IllegalStateException(table + ".id 必须为自增主键");
                if (name.equals("created_at") && rs.getString("COLUMN_DEF") == null)
                    throw new IllegalStateException(table + ".created_at 缺少默认时间");
                actual.add(name);
            }
        }
        if (!actual.equals(expected.keySet()))
            throw new IllegalStateException(table + " 不存在或缺少字段，请先执行 sql/bank_init.sql");
        List<String> keys = new ArrayList<>();
        try (ResultSet rs = meta.getPrimaryKeys(connection.getCatalog(), null, table)) {
            while (rs.next()) keys.add(rs.getString("COLUMN_NAME"));
        }
        if (!keys.equals(List.of("id"))) throw new IllegalStateException(table + " 的主键不兼容");
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
            st.setString(1, table);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next() || !"InnoDB".equalsIgnoreCase(rs.getString(1)))
                    throw new IllegalStateException(table + " 必须使用 InnoDB");
            }
        }
    }

    private static void foreignKey(Connection connection, String table, String column, String parent) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, table)) {
            while (rs.next()) {
                if (column.equals(rs.getString("FKCOLUMN_NAME")) && parent.equals(rs.getString("PKTABLE_NAME"))
                        && "id".equals(rs.getString("PKCOLUMN_NAME"))
                        && rs.getShort("DELETE_RULE") == DatabaseMetaData.importedKeyRestrict
                        && rs.getShort("UPDATE_RULE") == DatabaseMetaData.importedKeyRestrict) return;
            }
        }
        throw new IllegalStateException(table + " 缺少限制删除的 " + column + " 外键");
    }
}
