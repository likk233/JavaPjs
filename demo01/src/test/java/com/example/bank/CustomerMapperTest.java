package com.example.bank;

import com.example.entity.Customer;
import com.example.mapper.CustomerMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerMapperTest extends BankTestSupport {
    @Test void baseMapperCrudAndAnnotations() {
        try (var session = factory.openSession(false)) {
            var m = session.getMapper(CustomerMapper.class);
            var c = new Customer(null, "客户甲", "13800000000");
            assertEquals(1, m.insert(c)); assertNotNull(c.getId());
            assertEquals("客户甲", m.selectById(c.getId()).getName());
            Customer changes = new Customer(c.getId(), "新姓名", null);
            assertEquals(1, m.updateById(changes));
            assertEquals("13800000000", m.selectById(c.getId()).getPhone());
            assertEquals(1, m.deleteById(c.getId())); assertNull(m.selectById(c.getId()));
            session.rollback();
        }
    }

    private List<Customer> seeds() {
        var rows = List.of(new Customer(null, "张三", "138001"), new Customer(null, "张三", "139002"), new Customer(null, "李四", "138003"));
        assertEquals(3, bank.addCustomers(rows));
        assertTrue(rows.stream().allMatch(c -> c.getId() != null));
        return rows;
    }

    @Test void ifWhereAndNullOrBlankConditions() {
        var rows = seeds();
        assertEquals(3, bank.findCustomers(null).size());
        assertEquals(3, bank.findCustomers(new Customer(null, " ", "")).size());
        assertEquals(2, bank.findCustomers(new Customer(null, "张", null)).size());
        assertEquals(2, bank.findCustomers(new Customer(null, null, "138")).size());
        var found = bank.findCustomers(new Customer(null, "张", "138"));
        assertEquals(List.of(rows.get(0).getId()), found.stream().map(Customer::getId).toList());
        assertTrue(bank.findCustomers(new Customer(null, "不存在", null)).isEmpty());
        assertTrue(bank.findCustomers(new Customer(null, "' OR 1=1 --", null)).isEmpty());
    }

    @Test void chooseUsesNameThenPhoneThenAll() {
        seeds();
        assertEquals(2, bank.findCustomersByPriority("张三", "138003").size());
        assertEquals(0, bank.findCustomersByPriority("不存在", "138003").size());
        assertEquals("李四", bank.findCustomersByPriority(null, "138003").get(0).getName());
        assertEquals(3, bank.findCustomersByPriority(" ", "").size());
    }

    @Test void setOnlyChangesSuppliedFields() {
        var c = bank.addCustomer("客户", "138001");
        assertEquals(1, bank.updateCustomer(new Customer(c.getId(), "改名", null)));
        assertEquals("138001", bank.findCustomersByPriority("改名", null).get(0).getPhone());
        assertEquals(1, bank.updateCustomer(new Customer(c.getId(), null, "139002")));
        assertEquals("改名", bank.findCustomersByPriority(null, "139002").get(0).getName());
        assertEquals(1, bank.updateCustomer(new Customer(c.getId(), "再次改名", "137003")));
        assertEquals(0, bank.updateCustomer(new Customer(Integer.MAX_VALUE, "无", null)));
        assertThrows(IllegalArgumentException.class, () -> bank.updateCustomer(new Customer(c.getId(), null, null)));
        assertThrows(IllegalArgumentException.class, () -> bank.updateCustomer(new Customer(c.getId(), " ", null)));
        assertThrows(IllegalArgumentException.class, () -> bank.updateCustomer(null));
    }

    @Test void foreachInsertDeleteAndDuplicateNames() throws Exception {
        var rows = seeds();
        assertEquals(2, bank.deleteCustomers(List.of(rows.get(0).getId(), rows.get(2).getId())));
        assertEquals(1, count("bank_customer"));
    }

    @Test void batchDeleteWithAccountRollsBackEntireStatement() {
        var free = bank.addCustomer("无账户", "1"); var owner = bank.addCustomer("有账户", "2");
        bank.openAccount(owner.getId());
        assertThrows(PersistenceException.class, () -> bank.deleteCustomers(List.of(free.getId(), owner.getId())));
        assertEquals(2, bank.findCustomers(null).size());
    }

    @Test void invalidBatchDoesNotInsertPartialData() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> bank.addCustomers(List.of(new Customer(null, "有效", "1"), new Customer(null, "", "2"))));
        assertEquals(0, count("bank_customer"));
        for (List<Customer> rows : Arrays.asList(null, List.<Customer>of(), Arrays.asList((Customer)null)))
            assertThrows(IllegalArgumentException.class, () -> bank.addCustomers(rows));
        assertThrows(IllegalArgumentException.class, () -> bank.deleteCustomers(List.of()));
        assertThrows(IllegalArgumentException.class, () -> bank.deleteCustomers(null));
        assertThrows(IllegalArgumentException.class, () -> bank.deleteCustomers(Arrays.asList(1, null)));
        assertThrows(IllegalArgumentException.class, () -> bank.deleteCustomers(List.of(0)));
    }

    @Test void wrapperCombinesFiltersAndPagesStably() {
        var rows = seeds();
        var p = bank.customerPage(1, 1, "张", "13");
        assertEquals(2, p.getTotal()); assertEquals(rows.get(0).getId(), p.getRecords().get(0).getId());
        assertEquals(rows.get(1).getId(), bank.customerPage(2, 1, "张", "13").getRecords().get(0).getId());
        assertTrue(bank.customerPage(3, 1, "张", "13").getRecords().isEmpty());
        assertEquals(3, bank.customerPage(1, 5, null, " ").getTotal());
        assertEquals(0, bank.customerPage(1, 1, "无", null).getTotal());
        assertThrows(IllegalArgumentException.class, () -> bank.customerPage(0, 1, null, null));
        assertThrows(IllegalArgumentException.class, () -> bank.customerPage(1, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> bank.customerPage(Long.MAX_VALUE, 3, null, null));
    }
}
