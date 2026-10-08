package com.example.mapper;

import com.example.entity.User;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 对 UserMapper 的每个方法进行单元测试。
 * 通过 @Order 控制执行顺序：插入 -> 查询 -> 更新 -> 删除。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserMapperTest {

    private static final String TEST_USERNAME = "zhangsan";
    private static final String TEST_PASSWORD = "123456";
    private static final String NEW_PASSWORD = "654321";

    /** 保存测试插入的主键 id，供后续方法使用 */
    private static Integer insertedId;

    @Test
    @Order(1)
    void testInsert() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            // 清理历史残留，保证测试可重复执行
            List<User> existing = mapper.selectByUsername(TEST_USERNAME);
            for (User u : existing) {
                mapper.deleteById(u.getId());
            }

            User user = new User(null, TEST_USERNAME, TEST_PASSWORD, "zhangsan@qq.com");
            int rows = mapper.insert(user);
            assertEquals(1, rows, "插入应影响 1 行");
            assertNotNull(user.getId(), "自增主键应回填到 user.id");
            insertedId = user.getId();
        }
    }

    @Test
    @Order(2)
    void testSelectById() {
        assertNotNull(insertedId, "testInsert 未成功，无法执行查询测试");
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            User user = mapper.selectById(insertedId);
            assertNotNull(user, "按主键查询应有结果");
            assertEquals(TEST_USERNAME, user.getUsername());
        }
    }

    @Test
    @Order(3)
    void testSelectAll() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> users = mapper.selectAll();
            assertNotNull(users);
            assertFalse(users.isEmpty(), "user 表至少应有一条记录");
        }
    }

    @Test
    @Order(4)
    void testSelectByUsername() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> users = mapper.selectByUsername(TEST_USERNAME);
            assertNotNull(users);
            assertFalse(users.isEmpty(), "按用户名查询应有结果");
            for (User u : users) {
                assertEquals(TEST_USERNAME, u.getUsername());
            }
        }
    }

    @Test
    @Order(5)
    void testUpdateById() {
        assertNotNull(insertedId, "testInsert 未成功，无法执行更新测试");
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            User user = new User(insertedId, TEST_USERNAME, NEW_PASSWORD, "zhangsan@qq.com");
            int rows = mapper.updateById(user);
            assertEquals(1, rows, "更新应影响 1 行");

            User updated = mapper.selectById(insertedId);
            assertEquals(NEW_PASSWORD, updated.getPassword(), "密码应更新为新值");
        }
    }

    @Test
    @Order(6)
    void testDeleteById() {
        assertNotNull(insertedId, "testInsert 未成功，无法执行删除测试");
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            int rows = mapper.deleteById(insertedId);
            assertEquals(1, rows, "删除应影响 1 行");

            User user = mapper.selectById(insertedId);
            assertNull(user, "删除后按主键查询应为 null");
        }
    }

    @Test
    @Order(7)
    void testFindAll() throws Exception {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            // 重置表，使自增主键从 1 开始，保证输出与预期一致
            try (java.sql.Statement st = session.getConnection().createStatement()) {
                st.execute("TRUNCATE TABLE user");
            }

            UserMapper mapper = session.getMapper(UserMapper.class);

            mapper.insert(new User(null, "张三", "123", "zhangsan@qq.com"));
            mapper.insert(new User(null, "李四", "456", "lisi@qq.com"));
            mapper.insert(new User(null, "王五", "789", "wangwu@qq.com"));

            List<User> users = mapper.findAll();
            assertEquals(3, users.size(), "应查询到 3 条用户");
            for (User u : users) {
                System.out.println(u);
            }
        }
    }

    @Test
    @Order(8)
    void testSelectUserWithOrders() throws Exception {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            // 清理残留数据（先删订单，再删用户）
            try (java.sql.Statement st = session.getConnection().createStatement()) {
                st.execute("DELETE FROM orders");
                st.execute("DELETE FROM user");
            }

            UserMapper mapper = session.getMapper(UserMapper.class);
            User user = new User(null, "关联用户", "pass", "assoc@qq.com");
            mapper.insert(user);
            assertNotNull(user.getId(), "插入用户后主键应回填");

            // 通过 JDBC 为该用户插入两笔订单，构造一对多数据
            try (PreparedStatement ps = session.getConnection().prepareStatement(
                    "INSERT INTO orders (user_id, order_no, amount) VALUES (?, ?, ?)")) {
                ps.setInt(1, user.getId());
                ps.setString(2, "NO-A001");
                ps.setDouble(3, 100.50);
                ps.addBatch();

                ps.setInt(1, user.getId());
                ps.setString(2, "NO-A002");
                ps.setDouble(3, 200.00);
                ps.addBatch();

                ps.executeBatch();
            }

            User loaded = mapper.selectUserWithOrders(user.getId());
            assertNotNull(loaded, "关联查询应有结果");
            assertEquals("关联用户", loaded.getUsername());
            assertNotNull(loaded.getOrders(), "订单集合不应为 null");
            assertEquals(2, loaded.getOrders().size(), "应带出 2 笔订单");
            for (com.example.entity.Order o : loaded.getOrders()) {
                assertEquals(user.getId(), o.getUserId(), "订单应归属该用户");
                System.out.println(o);
            }
        }
    }
}
