package com.example.mapper;

import com.example.entity.Product;
import com.example.entity.Supplier;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多表联查测试：供应商 - 商品 - 客户。
 * 数据由 sql/shop_init.sql 初始化，请先执行该脚本。
 */
public class ShopQueryTest {

    @Test
    void testSelectSoldProducts() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);

            List<Product> sold = mapper.selectSoldProducts();
            assertNotNull(sold);
            // 种子数据中 4 件已售出（耳机未售出）
            assertEquals(4, sold.size(), "已售出商品应为 4 件");

            // 手机：买家张三，供应商为 华为/小米/苹果 三家，供应商服务的客户共 4 人
            Product phone = sold.stream()
                    .filter(p -> "手机".equals(p.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("应能查到手机"));

            assertNotNull(phone.getCustomer(), "手机应有买家");
            assertEquals("张三", phone.getCustomer().getName());
            assertEquals(3, phone.getSuppliers().size(), "手机应有三家供应商");
            assertEquals(4, phone.getServedCustomers().size(), "手机供应商服务的客户应为 4 人");

            System.out.println("===== 已销售商品 =====");
            for (Product p : sold) {
                System.out.println("商品: " + p.getName() + "，买家: " + p.getCustomer().getName());
                System.out.println("  供应商: " + p.getSuppliers());
                System.out.println("  供应商服务的客户: " + p.getServedCustomers());
            }
        }
    }

    @Test
    void testSelectSupplier() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            SupplierMapper mapper = session.getMapper(SupplierMapper.class);

            Supplier supplier = mapper.selectSupplier(1);
            assertNotNull(supplier);
            assertEquals("华为", supplier.getName());

            // 华为供应 4 件商品，其中未售出的耳机买家为 null
            assertEquals(4, supplier.getProducts().size(), "华为应供应 4 件商品");
            assertEquals(2, supplier.getCustomers().size(), "华为应服务 2 名客户");

            boolean hasUnsold = supplier.getProducts().stream()
                    .anyMatch(p -> "耳机".equals(p.getName()) && p.getCustomer() == null);
            assertTrue(hasUnsold, "耳机应尚未售出（买家为 null）");

            System.out.println("===== 供应商: " + supplier.getName() + " =====");
            System.out.println("供应的商品:");
            for (Product p : supplier.getProducts()) {
                String buyer = p.getCustomer() == null ? "未售出" : p.getCustomer().getName();
                System.out.println("  " + p.getName() + "（买家: " + buyer + "）");
            }
            System.out.println("服务的客户: " + supplier.getCustomers());
        }
    }

    @Test
    void testSelectServedCustomersByProductId() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            List<com.example.entity.Customer> customers = mapper.selectServedCustomersByProductId(1);
            assertNotNull(customers);
            assertFalse(customers.isEmpty(), "供应手机的供应商至少服务一名客户");
        }
    }

    @Test
    void testSelectServedCustomersBySupplierId() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            SupplierMapper mapper = session.getMapper(SupplierMapper.class);
            List<com.example.entity.Customer> customers = mapper.selectServedCustomersBySupplierId(1);
            assertNotNull(customers);
            assertEquals(2, customers.size(), "华为应服务 2 名客户");
        }
    }
}
