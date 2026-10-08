package com.example.entity;

/**
 * 身份证实体，与用户构成「一对一」关系：一张身份证只属于一个用户。
 */
public class IdCard {

    private Integer id;
    private String cardNo;
    private String address;
    /** 外键：所属用户 id（一对一，唯一） */
    private Integer userId;

    /** 一对一：所属用户 */
    private User user;

    public IdCard() {
    }

    public IdCard(Integer id, String cardNo, String address, Integer userId) {
        this.id = id;
        this.cardNo = cardNo;
        this.address = address;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "IdCard{" +
                "id=" + id +
                ", cardNo='" + cardNo + '\'' +
                ", address='" + address + '\'' +
                ", userId=" + userId +
                '}';
    }
}
