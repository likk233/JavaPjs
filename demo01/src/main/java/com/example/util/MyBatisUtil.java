package com.example.util;

import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * MyBatis 工具类：加载 mybatis-config.xml，维护全局唯一的 SqlSessionFactory。
 * 使用 MyBatis-Plus 的 MybatisSqlSessionFactoryBuilder，以启用 BaseMapper 等增强能力。
 */
public final class MyBatisUtil {

    private static final SqlSessionFactory sqlSessionFactory = createSqlSessionFactory(new Properties());

    /**
     * 创建会话工厂。传入的属性覆盖 db.properties，便于连接独立测试数据库。
     * 默认连接仍由 db.properties 配置。
     */
    public static SqlSessionFactory createSqlSessionFactory(Properties properties) {
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(in, properties);
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
            factory.getConfiguration().addInterceptor(interceptor);
            return factory;
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
