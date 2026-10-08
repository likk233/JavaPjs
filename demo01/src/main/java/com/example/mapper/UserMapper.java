package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.FetchType;
import java.util.List;

/**
 * 用户数据访问接口（Mapper）。
 * XML 和注解共同配置 SQL 与关联查询。
 */
public interface UserMapper {

    /** 新增用户，成功后主键回填到 user.id */
    int insert(User user);

    /** 根据主键查询 */
    User selectById(Integer id);

    /** 查询所有用户 */
    List<User> selectAll();

    /** 查询所有用户（含 email），供 testFindAll 使用 */
    List<User> findAll();

    /** 根据用户名查询（演示条件查询） */
    List<User> selectByUsername(String username);

    /** 一对多关联：查询用户，访问 getOrders() 时延迟加载订单列表。 */
    User selectUserWithOrders(Integer id);

    /** 一对一关联：查询用户，访问 getIdCard() 时延迟加载身份证。 */
    User selectUserWithIdCard(Integer id);

    /** 注解中单独开启一对一、一对多延迟加载，覆盖全局 lazyLoadingEnabled 配置。 */
    @Select("SELECT id, username, password, email FROM user WHERE id = #{id}")
    @Results(id = "userWithRelationsLazyMap", value = {
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "username", property = "username"),
            @Result(column = "password", property = "password"),
            @Result(column = "email", property = "email"),
            @Result(column = "id", property = "idCard",
                    one = @One(select = "com.example.mapper.IdCardMapper.selectCardByUserId",
                            fetchType = FetchType.LAZY)),
            @Result(column = "id", property = "orders",
                    many = @Many(select = "com.example.mapper.OrderMapper.selectByUserId",
                            fetchType = FetchType.LAZY))
    })
    User selectUserWithRelationsLazy(Integer id);

    /** 根据主键更新（以 id 为条件，更新非空字段） */
    int updateById(User user);

    /** 根据主键删除 */
    int deleteById(Integer id);
}
