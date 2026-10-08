package com.example.mapper;

import com.example.entity.Customer;
import com.example.entity.Product;
import java.util.List;

/**
 * 商品数据访问接口。
 * 对应多表联查需求一：已销售的商品及其关联信息。
 */
public interface ProductMapper {

    /**
     * 查询所有已售出的商品，每个商品带出：
     * - suppliers：由哪些供应商提供（多对多）
     * - customer：被哪个客户买走（多对一）
     * - servedCustomers：这些供应商为哪些客户提供了服务（间接多对多）
     */
    List<Product> selectSoldProducts();

    /**
     * 供嵌套 select 使用：查询「供应了指定商品的供应商」所服务的全部客户。
     */
    List<Customer> selectServedCustomersByProductId(Integer productId);
}
