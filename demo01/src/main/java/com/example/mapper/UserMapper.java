package com.example.mapper;

import com.example.entity.User;
import java.util.List;

/**
 * 用户数据访问接口（Mapper）。
 * 每个方法与 UserMapper.xml 中的 SQL 语句一一对应。
 */
public interface UserMapper {

    /** 新增用户，成功后主键回填到 user.id */
    int insert(User user);

    /** 根据主键查询 */
    User selectById(Integer id);

    /** 查询所有用户 */
    List<User> selectAll();

    /** 根据用户名查询（演示条件查询） */
    List<User> selectByUsername(String username);

    /** 根据主键更新（以 id 为条件，更新非空字段） */
    int updateById(User user);

    /** 根据主键删除 */
    int deleteById(Integer id);
}
