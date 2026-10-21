package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.Customer;
import com.example.util.BankValidation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 通用 CRUD、动态 SQL 和条件构造器的银行客户示例。写操作由 Service 管理事务。 */
public interface CustomerMapper extends BaseMapper<Customer> {
    List<Customer> selectByCondition(@Param("condition") Customer condition);

    List<Customer> selectByPriority(@Param("name") String name, @Param("phone") String phone);

    int updateFields(@Param("customer") Customer customer);

    default int updateSelective(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("客户不能为空");
        BankValidation.id(customer.getId());
        if (customer.getName() == null && customer.getPhone() == null)
            throw new IllegalArgumentException("至少提供一个更新字段");
        if (customer.getName() != null)
            customer.setName(BankValidation.text(customer.getName(), "姓名", 50));
        if (customer.getPhone() != null)
            customer.setPhone(BankValidation.text(customer.getPhone(), "电话", 30));
        return updateFields(customer);
    }

    int insertRows(@Param("customers") List<Customer> customers);

    default int insertBatch(List<Customer> customers) {
        BankValidation.nonEmpty(customers);
        for (Customer c : customers) {
            c.setName(BankValidation.text(c.getName(), "姓名", 50));
            c.setPhone(BankValidation.text(c.getPhone(), "电话", 30));
        }
        return insertRows(customers);
    }

    int deleteRows(@Param("ids") List<Integer> ids);

    default int deleteBatch(List<Integer> ids) {
        BankValidation.nonEmpty(ids);
        ids.forEach(BankValidation::id);
        // 外键禁止删除已有账户的客户；InnoDB 单条 DELETE 不会部分成功。
        return deleteRows(ids);
    }

    default Page<Customer> selectCustomerPage(long current, long size, String name, String phone) {
        BankValidation.page(current, size);
        return selectPage(new Page<>(current, size), Wrappers.<Customer>lambdaQuery()
                .like(name != null && !name.isBlank(), Customer::getName, name)
                .like(phone != null && !phone.isBlank(), Customer::getPhone, phone)
                .orderByAsc(Customer::getId));
    }
}
