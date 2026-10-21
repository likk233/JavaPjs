package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;

/** 银行业务实体；金额使用精确十进制。 */
@TableName("bank_customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String phone;

    public Customer() {}

    public Customer(Integer id, String name, String phone) {
        this.id = id; this.name = name; this.phone = phone;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return "Customer{" + "id=" + id + ", " + "name=" + name + ", " + "phone=" + phone + "}";
    }
}
