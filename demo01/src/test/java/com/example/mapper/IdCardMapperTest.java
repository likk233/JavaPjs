package com.example.mapper;

import com.example.entity.IdCard;
import com.example.entity.User;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 用户与身份证双向的一对一关联测试，使用 db.properties 配置的 MySQL。
 * user 和 id_card 表需已存在并使用 InnoDB；所有测试数据在测试结束时回滚。
 */
public class IdCardMapperTest {

    private SqlSession session;
    private UserMapper userMapper;
    private IdCardMapper idCardMapper;
    private User user;
    private User userWithoutCard;
    private IdCard idCard;

    @BeforeEach
    void setUp() {
        session = MyBatisUtil.getSqlSessionFactory().openSession(false);
        userMapper = session.getMapper(UserMapper.class);
        idCardMapper = session.getMapper(IdCardMapper.class);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        user = new User(null, "idcard-" + suffix, "123456", "idcard@example.com");
        userWithoutCard = new User(null, "no-card-" + suffix, "654321", "no-card@example.com");
        insertUserWithoutIdCard(user);
        insertUserWithoutIdCard(userWithoutCard);
        idCard = new IdCard(null, "110101199001011234", "北京市朝阳区", user.getId());
        idCardMapper.insert(idCard);
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
        IdCard card = new IdCard(null, "310101199202022345", null, userWithoutCard.getId());
        assertEquals(1, idCardMapper.insert(card));
        assertNotNull(card.getId(), "身份证主键应回填");
        assertNotNull(user.getId(), "用户主键应回填");
        User loaded = userMapper.selectUserWithIdCard(userWithoutCard.getId());
        assertEquals(card.getId(), loaded.getIdCard().getId());
        assertEquals(card.getCardNo(), loaded.getIdCard().getCardNo());
        assertNull(loaded.getIdCard().getAddress());
    }

    @Test
    void testSelectUserWithIdCard() {
        // 给另一个用户绑定不同身份证，验证关联不会串到其他用户。
        IdCard otherCard = new IdCard(null, "310101199202022345", "上海市浦东新区", userWithoutCard.getId());
        idCardMapper.insert(otherCard);
        User loaded = userMapper.selectUserWithIdCard(user.getId());
        assertNotNull(loaded);
        assertEquals(user.getId(), loaded.getId());
        assertEquals(user.getUsername(), loaded.getUsername());
        assertEquals(user.getPassword(), loaded.getPassword());
        assertEquals(user.getEmail(), loaded.getEmail());
        assertNotNull(loaded.getIdCard(), "一对一查询应带出身份证");
        assertEquals(idCard.getId(), loaded.getIdCard().getId());
        assertEquals(idCard.getCardNo(), loaded.getIdCard().getCardNo());
        assertEquals(idCard.getAddress(), loaded.getIdCard().getAddress());
        assertEquals(user.getId(), loaded.getIdCard().getUserId());
        assertEquals(otherCard.getId(),
                userMapper.selectUserWithIdCard(userWithoutCard.getId()).getIdCard().getId());
    }

    @Test
    void testSelectUserWithoutIdCard() {
        User loaded = userMapper.selectUserWithIdCard(userWithoutCard.getId());
        assertNotNull(loaded, "关联查询应保留没有身份证的用户");
        assertEquals(userWithoutCard.getUsername(), loaded.getUsername());
        assertNull(loaded.getIdCard(), "没有身份证时应返回 null");
        assertNull(idCardMapper.selectByUserId(userWithoutCard.getId()));
    }

    @Test
    void testSelectMissingUser() {
        assertNull(userMapper.selectUserWithIdCard(-1));
        assertNull(idCardMapper.selectById(-1));
        assertNull(idCardMapper.selectByIdNested(-1));
    }

    @Test
    void testSelectById() {
        IdCard loaded = idCardMapper.selectById(idCard.getId());
        assertCardWithUser(loaded);
    }

    @Test
    void testSelectByIdNested() {
        IdCard loaded = idCardMapper.selectByIdNested(idCard.getId());
        assertCardWithUser(loaded);
    }

    @Test
    void testSelectByUserId() {
        IdCard loaded = idCardMapper.selectByUserId(user.getId());
        assertCardWithUser(loaded);
    }

    @Test
    void testUserCannotHaveTwoIdCards() {
        IdCard duplicate = new IdCard(null, "440101199303033456", "广州市天河区", user.getId());
        assertThrows(PersistenceException.class, () -> idCardMapper.insert(duplicate),
                "user_id 唯一约束应禁止同一用户绑定两张身份证");
        assertEquals(idCard.getId(), userMapper.selectUserWithIdCard(user.getId()).getIdCard().getId());
    }

    private void insertUserWithoutIdCard(User candidate) {
        // 历史数据可能含有指向已删除用户的身份证，跳过这些已占用的关联 ID。
        // 这里只删除本测试刚插入的用户，不删除已有身份证记录。
        do {
            candidate.setId(null);
            userMapper.insert(candidate);
            if (idCardMapper.selectByUserId(candidate.getId()) == null) {
                return;
            }
            userMapper.deleteById(candidate.getId());
        } while (true);
    }

    private void assertCardWithUser(IdCard loaded) {
        assertNotNull(loaded);
        assertEquals(idCard.getId(), loaded.getId());
        assertEquals(idCard.getCardNo(), loaded.getCardNo());
        assertEquals(idCard.getAddress(), loaded.getAddress());
        assertEquals(user.getId(), loaded.getUserId());
        assertNotNull(loaded.getUser(), "身份证查询应带出所属用户");
        assertEquals(user.getId(), loaded.getUser().getId());
        assertEquals(user.getUsername(), loaded.getUser().getUsername());
        assertEquals(user.getEmail(), loaded.getUser().getEmail());
    }
}
