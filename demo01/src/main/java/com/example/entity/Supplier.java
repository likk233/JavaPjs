package com.example.entity;

import java.util.List;

/**
 * 供应商实体。
 * 与商品、客户均为多对多关系（通过中间表 supplier_product / supplier_customer）。
 */
public class Supplier {

    private Integer id;
    private String name;
    private String contact;

    /** 多对多：该供应商供应的商品 */
    private List<Product> products;

    /** 多对多：该供应商服务的客户 */
    private List<Customer> customers;

    public Supplier() {
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

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contact='" + contact + '\'' +
                '}';
    }
}
