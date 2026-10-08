package com.example.mapper;

import com.example.entity.Customer;
import com.example.entity.Supplier;
import java.util.List;

/**
 * 供应商数据访问接口。
 * 对应多表联查需求二：供应商及其关联信息。
 */
public interface SupplierMapper {

    /**
     * 按主键查询供应商，带出：
     * - products：卖了哪些商品（多对多，每个商品带买家客户）
     * - customers：为哪些客户提供了服务（多对多）
     */
    Supplier selectSupplier(Integer id);

    /**
     * 供嵌套 select 使用：查询指定供应商服务的全部客户。
     */
    List<Customer> selectServedCustomersBySupplierId(Integer supplierId);
}
