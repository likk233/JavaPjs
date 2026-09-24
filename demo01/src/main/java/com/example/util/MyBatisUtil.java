package com.example.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * MyBatis 工具类：加载 mybatis-config.xml，维护全局唯一的 SqlSessionFactory。
 */
public final class MyBatisUtil {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(in);
        } catch (IOException e) {
            throw new RuntimeException("加载 mybatis-config.xml 失败", e);
        }
    }

    private MyBatisUtil() {
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}
