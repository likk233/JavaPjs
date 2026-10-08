package com.example.mapper;

import com.example.entity.Order;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 订单查询接口，用于用户的一对多延迟加载。 */
public interface OrderMapper {

    @Select("SELECT id, user_id, order_no, amount FROM orders WHERE user_id = #{userId} ORDER BY id")
    List<Order> selectByUserId(Integer userId);
}
