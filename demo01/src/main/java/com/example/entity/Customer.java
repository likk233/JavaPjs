package com.example.entity;

import java.util.List;

/**
 * 客户实体。
 * - 与商品：一对多（一个客户买多个商品）
 * - 与供应商：多对多（供应商为客户提供服务，中间表 supplier_customer）
 */
public class Customer {

    private Integer id;
    private String name;
    private String phone;

    /** 一对多：该客户买走的商品 */
    private List<Product> products;

    /** 多对多：为该客户提供服务的供应商 */
    private List<Supplier> suppliers;

    public Customer() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<Supplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<Supplier> suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
