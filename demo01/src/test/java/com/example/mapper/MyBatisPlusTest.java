package com.example.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.Order;
import com.example.entity.IdCard;
import com.example.entity.User;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用 db.properties 配置的 MySQL 数据库；user 表需已存在并使用 InnoDB。
 * 每个测试使用唯一用户名创建数据，并在结束时回滚，不依赖固定的表记录数。
 */
public class MyBatisPlusTest {

    private SqlSession session;
    private UserPlusMapper mapper;
    private User firstUser;
    private User secondUser;
    private String usernamePrefix;
    private long totalCount;

    @BeforeEach
    void setUp() {
        session = MyBatisUtil.getSqlSessionFactory().openSession(false);
        mapper = session.getMapper(UserPlusMapper.class);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        usernamePrefix = "mp-" + suffix + "-";
        firstUser = new User(null, usernamePrefix + "alice", "123456", "alice@example.com");
        secondUser = new User(null, usernamePrefix + "bob", "123456", "bob@example.com");
        mapper.insert(firstUser);
        mapper.insert(secondUser);
        mapper.insert(new User(null, "other-" + suffix, "123456", "other@example.com"));
        totalCount = mapper.selectCount(null);
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            try {
                session.rollback();
            } finally {
                session.close();
            }
        }
    }

    @Test
    void testInsert() {
        User user = new User(null, usernamePrefix + "new", "123456", "new@example.com");
        user.setOrders(List.of(new Order()));
        user.setIdCard(new IdCard(null, "test-card", "测试地址", null));
        assertEquals(1, mapper.insert(user));
        assertNotNull(user.getId(), "自增主键应回填");
        assertEquals(user.getUsername(), mapper.selectById(user.getId()).getUsername());
        assertNull(mapper.selectById(user.getId()).getOrders(), "关联集合不参与表字段映射");
        assertNull(mapper.selectById(user.getId()).getIdCard(), "身份证不参与 user 表字段映射");
    }

    @Test
    void testSelectById() {
        assertEquals(firstUser.getUsername(), mapper.selectById(firstUser.getId()).getUsername());
        assertNull(mapper.selectById(-1));
    }

    @Test
    void testSelectListWithWrapper() {
        List<User> users = mapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, firstUser.getUsername()));
        assertEquals(1, users.size());
        assertEquals(firstUser.getId(), users.get(0).getId());
        assertEquals(2L, mapper.selectCount(Wrappers.<User>lambdaQuery()
                .likeRight(User::getUsername, usernamePrefix)));
    }

    @Test
    void testSelectByUsername() {
        mapper.insert(new User(null, firstUser.getUsername(), "another-password", null));
        assertEquals(2, mapper.selectByUsername(firstUser.getUsername()).size());
        assertTrue(mapper.selectByUsername(usernamePrefix + "missing").isEmpty());
        assertTrue(mapper.selectByUsername("' OR 1=1 --").isEmpty());
        assertThrows(NullPointerException.class, () -> mapper.selectByUsername(null));
    }

    @Test
    void testSelectByUsernameLike() {
        assertEquals(List.of(firstUser.getId(), secondUser.getId()),
                mapper.selectByUsernameLike(usernamePrefix).stream().map(User::getId).toList());
        assertEquals(totalCount, mapper.selectByUsernameLike(null).size());
        assertEquals(totalCount, mapper.selectByUsernameLike(" ").size());
        assertTrue(mapper.selectByUsernameLike(usernamePrefix + "missing").isEmpty());
    }

    @Test
    void testUpdateById() {
        User changes = new User();
        changes.setId(firstUser.getId());
        changes.setPassword("new-password");
        assertEquals(1, mapper.updateById(changes));
        User updated = mapper.selectById(firstUser.getId());
        assertEquals("new-password", updated.getPassword());
        assertEquals(firstUser.getUsername(), updated.getUsername(), "未设置的字段保留原值");
        assertEquals("alice@example.com", updated.getEmail());
    }

    @Test
    void testUpdateEmailById() {
        assertEquals(1, mapper.updateEmailById(firstUser.getId(), "updated@example.com"));
        assertEquals("updated@example.com", mapper.selectById(firstUser.getId()).getEmail());
        assertEquals("bob@example.com", mapper.selectById(secondUser.getId()).getEmail());
        assertEquals(1, mapper.updateEmailById(firstUser.getId(), null));
        assertNull(mapper.selectById(firstUser.getId()).getEmail());
        assertEquals(firstUser.getUsername(), mapper.selectById(firstUser.getId()).getUsername());
        assertEquals(0, mapper.updateEmailById(-1, "missing@example.com"));
        assertThrows(NullPointerException.class, () -> mapper.updateEmailById(null, "email"));
    }

    @Test
    void testDeleteById() {
        assertEquals(1, mapper.deleteById(firstUser.getId()));
        assertNull(mapper.selectById(firstUser.getId()));
        assertNotNull(mapper.selectById(secondUser.getId()));
        assertEquals(0, mapper.deleteById(firstUser.getId()));
    }

    @Test
    void testSelectUserPage() {
        Page<User> page = mapper.selectUserPage(2, 1, usernamePrefix);
        assertEquals(2, page.getTotal(), "总数应包含全部匹配记录");
        assertEquals(2, page.getPages());
        assertEquals(2, page.getCurrent());
        assertEquals(1, page.getSize());
        assertEquals(1, page.getRecords().size(), "数据库应执行分页而非返回全部记录");
        assertEquals(secondUser.getId(), page.getRecords().get(0).getId());

        Page<User> all = mapper.selectUserPage(1, 2, null);
        assertEquals(totalCount, all.getTotal());
        assertEquals(2, all.getRecords().size());
        assertEquals(totalCount, mapper.selectUserPage(1, 2, " ").getTotal());

        Page<User> missing = mapper.selectUserPage(1, 2, usernamePrefix + "missing");
        assertEquals(0, missing.getTotal());
        assertTrue(missing.getRecords().isEmpty());
        Page<User> beyondLast = mapper.selectUserPage(3, 1, usernamePrefix);
        assertEquals(2, beyondLast.getTotal());
        assertTrue(beyondLast.getRecords().isEmpty());
    }

    @Test
    void testInvalidPage() {
        assertThrows(IllegalArgumentException.class, () -> mapper.selectUserPage(0, 10, null));
        assertThrows(IllegalArgumentException.class, () -> mapper.selectUserPage(1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> mapper.selectUserPage(-1, 10, null));
        assertThrows(IllegalArgumentException.class, () -> mapper.selectUserPage(1, -1, null));
    }

    @Test
    void testExistingXmlMapperStillWorks() {
        UserMapper xmlMapper = session.getMapper(UserMapper.class);
        assertEquals(firstUser.getUsername(), xmlMapper.selectById(firstUser.getId()).getUsername());
        assertEquals(totalCount, xmlMapper.selectAll().size());
    }
}
