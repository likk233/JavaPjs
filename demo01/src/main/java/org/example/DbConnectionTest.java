package org.example;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * 数据库连接测试：读取 db.properties 并尝试连接 MySQL。
 */
public class DbConnectionTest {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (InputStream in = DbConnectionTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("找不到 db.properties");
            }
            props.load(in);
        }

        String driver = props.getProperty("jdbc.driver");
        String url = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");

        Class.forName(driver);
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ 数据库连接成功！");
            System.out.println("   URL      : " + url);
            System.out.println("   用户名   : " + username);
            System.out.println("   数据库   : " + conn.getCatalog());
            System.out.println("   MySQL 版本: " + conn.getMetaData().getDatabaseProductVersion());
        }
    }
}
