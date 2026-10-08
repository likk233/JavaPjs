package com.example.mapper;

import com.example.entity.IdCard;
import com.example.entity.Order;
import com.example.entity.User;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 MySQL 和实际 SQL 执行次数验证延迟加载；测试数据在结束时回滚。 */
public class LazyLoadingTest {

    private SqlSessionFactory factory;
    private SqlSession session;
    private UserMapper userMapper;
    private SqlCounter sqlCounter;
    private User user;
    private User userWithoutRelations;
    private IdCard idCard;

    @BeforeEach
    void setUp() throws Exception {
        // 单独创建工厂，SQL 计数插件不影响其他测试或应用的全局会话工厂。
        factory = MyBatisUtil.createSqlSessionFactory(new Properties());
        sqlCounter = new SqlCounter();
        factory.getConfiguration().addInterceptor(sqlCounter);
        session = factory.openSession(false);
        userMapper = session.getMapper(UserMapper.class);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        user = new User(null, "lazy-" + suffix, "123456", "lazy@example.com");
        userWithoutRelations = new User(null, "empty-" + suffix, "123456", null);
        insertUserWithoutRelations(user);
        insertUserWithoutRelations(userWithoutRelations);
        idCard = new IdCard(null, "110101199001011234", "北京市朝阳区", user.getId());
        session.getMapper(IdCardMapper.class).insert(idCard);
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "INSERT INTO orders (user_id, order_no, amount) VALUES (?, ?, ?)")) {
            statement.setInt(1, user.getId());
            statement.setString(2, "lazy-first-" + suffix);
            statement.setDouble(3, 100.50);
            statement.executeUpdate();
            statement.setString(2, "lazy-second-" + suffix);
            statement.setDouble(3, 200.00);
            statement.executeUpdate();
        }
        // 清空准备数据时产生的缓存，确保关联查询真的需要执行 SQL。
        session.clearCache();
        sqlCounter.reset();
    }

    @AfterEach
    void tearDown() {
        try {
            if (session != null) {
                try {
                    session.rollback();
                } finally {
                    session.close();
                }
            }
        } finally {
            if (factory != null) {
                ((PooledDataSource) factory.getConfiguration().getEnvironment()
                        .getDataSource()).forceCloseAll();
            }
        }
    }

    @Test
    void testXmlIdCardUserLoadsOnAccess() {
        IdCard loaded = session.getMapper(IdCardMapper.class).selectByIdNested(idCard.getId());
        assertEquals(1, sqlCounter.count(), "只执行身份证查询");
        assertEquals(idCard.getCardNo(), loaded.getCardNo());
        assertEquals(1, sqlCounter.count(), "访问普通属性不加载用户");
        assertEquals(user.getUsername(), loaded.getUser().getUsername());
        assertEquals(2, sqlCounter.count(), "首次访问 user 才查询用户");
        assertEquals(user.getId(), loaded.getUser().getId());
        assertEquals(2, sqlCounter.count(), "重复访问关联属性不重复查询");
    }

    @Test
    void testXmlUserIdCardLoadsOnAccess() {
        User loaded = userMapper.selectUserWithIdCard(user.getId());
        assertEquals(1, sqlCounter.count());
        assertEquals(user.getUsername(), loaded.getUsername());
        assertEquals(1, sqlCounter.count());
        assertEquals(idCard.getCardNo(), loaded.getIdCard().getCardNo());
        assertEquals(user.getId(), loaded.getIdCard().getUserId(), "下划线列名映射到驼峰字段");
        assertEquals(2, sqlCounter.count());
    }

    @Test
    void testXmlUserOrdersLoadOnAccess() {
        User loaded = userMapper.selectUserWithOrders(user.getId());
        assertEquals(1, sqlCounter.count());
        assertEquals(user.getUsername(), loaded.getUsername());
        assertEquals(1, sqlCounter.count());
        assertOrders(loaded.getOrders());
        assertEquals(2, sqlCounter.count());
        assertEquals(2, loaded.getOrders().size());
        assertEquals(2, sqlCounter.count());
    }

    @Test
    void testAnnotationOneAndManyLoadIndependently() {
        User loaded = userMapper.selectUserWithRelationsLazy(user.getId());
        assertEquals(1, sqlCounter.count(), "@One 和 @Many 均未查询");
        assertEquals(user.getUsername(), loaded.getUsername());
        assertEquals(1, sqlCounter.count());
        assertEquals(idCard.getCardNo(), loaded.getIdCard().getCardNo());
        assertEquals(2, sqlCounter.count(), "访问身份证不加载订单");
        assertOrders(loaded.getOrders());
        assertEquals(3, sqlCounter.count(), "访问订单才执行第三条查询");
        assertNotNull(loaded.getIdCard());
        assertEquals(2, loaded.getOrders().size());
        assertEquals(3, sqlCounter.count());
    }

    @Test
    void testAnnotationManyDoesNotLoadOne() {
        User loaded = userMapper.selectUserWithRelationsLazy(user.getId());
        assertOrders(loaded.getOrders());
        assertEquals(2, sqlCounter.count(), "访问订单不加载身份证");
        assertEquals(idCard.getCardNo(), loaded.getIdCard().getCardNo());
        assertEquals(3, sqlCounter.count());
    }

    @Test
    void testMissingRelationsLoadAsNullAndEmptyList() {
        User loaded = userMapper.selectUserWithRelationsLazy(userWithoutRelations.getId());
        assertNotNull(loaded);
        assertEquals(1, sqlCounter.count());
        assertNull(loaded.getIdCard());
        assertEquals(2, sqlCounter.count());
        assertTrue(loaded.getOrders().isEmpty());
        assertEquals(3, sqlCounter.count());
        assertNull(loaded.getIdCard());
        assertTrue(loaded.getOrders().isEmpty());
        assertEquals(3, sqlCounter.count(), "空结果也不重复查询");
    }

    @Test
    void testMissingUserDoesNotQueryRelations() {
        assertNull(userMapper.selectUserWithRelationsLazy(-1));
        assertEquals(1, sqlCounter.count());
    }

    @Test
    void testToStringTriggersLazyLoadingByDefault() {
        User loaded = userMapper.selectUserWithRelationsLazy(user.getId());
        assertEquals(1, sqlCounter.count());
        String description = loaded.toString();
        assertTrue(description.contains(idCard.getCardNo()));
        assertEquals(3, sqlCounter.count(), "默认 toString 会触发所有延迟加载关联");
    }

    private void insertUserWithoutRelations(User candidate) {
        do {
            candidate.setId(null);
            userMapper.insert(candidate);
            if (session.getMapper(IdCardMapper.class).selectCardByUserId(candidate.getId()) == null
                    && session.getMapper(OrderMapper.class).selectByUserId(candidate.getId()).isEmpty()) {
                return;
            }
            // 仅删除本测试刚插入的用户，跳过历史关联记录已占用的 ID。
            userMapper.deleteById(candidate.getId());
        } while (true);
    }

    private void assertOrders(List<Order> orders) {
        assertEquals(2, orders.size());
        assertEquals(user.getId(), orders.get(0).getUserId());
        assertEquals(user.getId(), orders.get(1).getUserId());
        assertEquals(100.50, orders.get(0).getAmount());
        assertEquals(200.00, orders.get(1).getAmount());
        assertTrue(orders.get(0).getId() < orders.get(1).getId());
    }

    /** 记录 JDBC 实际准备的 SELECT，避免缓存让测试误判为延迟加载。 */
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare",
            args = {Connection.class, Integer.class}))
    private static class SqlCounter implements Interceptor {
        private int count;

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            String sql = ((StatementHandler) invocation.getTarget()).getBoundSql().getSql();
            Object result = invocation.proceed();
            if (sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) {
                count++;
            }
            return result;
        }

        int count() {
            return count;
        }

        void reset() {
            count = 0;
        }
    }
}
