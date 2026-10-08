package com.example.mapper;

import com.example.entity.IdCard;
import org.apache.ibatis.annotations.Select;

/**
 * 身份证数据访问接口，查询身份证及其所属用户。
 * 用户及其身份证由 UserMapper.selectUserWithIdCard 查询。
 */
public interface IdCardMapper {

    /** 新增身份证，成功后主键回填到 idCard.id */
    int insert(IdCard idCard);

    /** 一对一（嵌套结果）：按主键查身份证，LEFT JOIN 带出所属用户 */
    IdCard selectById(Integer id);

    /** 一对一（嵌套 select）：先查身份证，再单独查所属用户 */
    IdCard selectByIdNested(Integer id);

    /** 按用户 id 查该用户的身份证（一对一） */
    IdCard selectByUserId(Integer userId);

    /** 只查询身份证，用于 User.idCard 的嵌套查询，避免循环加载所属用户。 */
    @Select("SELECT id, card_no, address, user_id FROM id_card WHERE user_id = #{userId}")
    IdCard selectCardByUserId(Integer userId);
}
