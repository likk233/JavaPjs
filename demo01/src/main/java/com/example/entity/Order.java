package com.example.entity;

/**
 * 订单实体，与 user 表构成「一对多」关系：一个用户拥有多笔订单。
 */
public class Order {

    private Integer id;
    private Integer userId;
    private String orderNo;
    private Double amount;

    public Order() {
    }

    public Order(Integer id, Integer userId, String orderNo, Double amount) {
        this.id = id;
        this.userId = userId;
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", userId=" + userId +
                ", orderNo='" + orderNo + '\'' +
                ", amount=" + amount +
                '}';
    }
}
