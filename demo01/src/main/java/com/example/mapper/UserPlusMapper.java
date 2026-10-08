package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.User;

import java.util.List;
import java.util.Objects;

/**
 * 基于 MyBatis-Plus 的用户 Mapper。
 * 继承 BaseMapper 即可获得 selectById / selectList / insert / updateById / deleteById 等通用 CRUD，
 * 无需编写 XML。
 */
public interface UserPlusMapper extends BaseMapper<User> {

    /** 按用户名精确查询；用户名可以重复，因此返回列表。 */
    default List<User> selectByUsername(String username) {
        Objects.requireNonNull(username, "用户名不能为空");
        return selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)
                .orderByAsc(User::getId));
    }

    /** 按用户名模糊查询，空关键词表示查询所有用户。 */
    default List<User> selectByUsernameLike(String keyword) {
        return selectList(Wrappers.<User>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), User::getUsername, keyword)
                .orderByAsc(User::getId));
    }

    /** 分页查询，可按用户名模糊筛选；页码从 1 开始，按主键稳定排序。 */
    default Page<User> selectUserPage(long current, long size, String keyword) {
        if (current < 1 || size < 1) {
            throw new IllegalArgumentException("页码和每页条数必须大于 0");
        }
        return selectPage(new Page<>(current, size), Wrappers.<User>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), User::getUsername, keyword)
                .orderByAsc(User::getId));
    }

    /** 只更新指定用户的邮箱，传 null 可清空邮箱。 */
    default int updateEmailById(Integer id, String email) {
        Objects.requireNonNull(id, "用户 ID 不能为空");
        return update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getEmail, email));
    }
}
