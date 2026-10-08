package com.example.entity;

import java.util.List;

/**
 * 商品实体。
 * - 与供应商：多对多（中间表 supplier_product）
 * - 与客户：多对一（一个客户买多个商品，一个商品只卖给一个客户，product.customer_id）
 */
public class Product {

    private Integer id;
    private String name;
    private Double price;
    /** 外键：买走该商品的客户 id，NULL 表示未售出 */
    private Integer customerId;

    /** 多对多：供应该商品的供应商 */
    private List<Supplier> suppliers;

    /** 多对一：买走该商品的客户 */
    private Customer customer;

    /** 该商品的供应商所服务的客户（经 supplier_product -> supplier_customer 间接关联） */
    private List<Customer> servedCustomers;

    public Product() {
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public List<Supplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<Supplier> suppliers) {
        this.suppliers = suppliers;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<Customer> getServedCustomers() {
        return servedCustomers;
    }

    public void setServedCustomers(List<Customer> servedCustomers) {
        this.servedCustomers = servedCustomers;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", customerId=" + customerId +
                '}';
    }
}
