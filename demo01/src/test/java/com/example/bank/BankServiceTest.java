package com.example.bank;

import com.example.entity.*;
import com.example.mapper.*;
import com.example.service.BankService;
import com.example.util.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest extends BankTestSupport {
    private static BigDecimal money(String value) { return new BigDecimal(value); }

    @Test void newCustomerAndExistingCustomerOpenMultipleZeroBalanceAccounts() throws Exception {
        var first = bank.openAccount("银行客户", "13800138000");
        var second = bank.openAccount(first.getCustomerId());
        assertNotEquals(first.getAccountNo(), second.getAccountNo());
        assertNotNull(first.getId()); assertNotNull(first.getCreatedAt());
        assertEquals(money("0.00"), first.getBalance()); assertEquals(money("0.00"), second.getBalance());
        assertEquals(1, count("bank_customer")); assertEquals(2, bank.accounts(first.getCustomerId()).size());
        assertEquals(0, bank.transactions(first.getAccountNo(), 1, 10).getTotal());
    }

    @Test void depositWithdrawAndFullWithdrawalHaveConsistentLedger() {
        var a = bank.openAccount("客户", "1");
        var deposit = bank.deposit(a.getAccountNo(), money("100.50"));
        assertEquals(TransactionType.DEPOSIT, deposit.getType());
        assertEquals(money("0.00"), deposit.getBalanceBefore()); assertEquals(money("100.50"), deposit.getBalanceAfter());
        assertNotNull(deposit.getId()); assertNotNull(deposit.getCreatedAt());
        var withdrawal = bank.withdraw(a.getAccountNo(), money("20.25"));
        assertEquals(TransactionType.WITHDRAW, withdrawal.getType());
        assertEquals(money("100.50"), withdrawal.getBalanceBefore()); assertEquals(money("80.25"), withdrawal.getBalanceAfter());
        assertEquals(money("80.25"), bank.account(a.getAccountNo()).getBalance());
        bank.withdraw(a.getAccountNo(), money("80.25"));
        assertEquals(money("0.00"), bank.account(a.getAccountNo()).getBalance());
        var page = bank.transactions(a.getAccountNo(), 1, 2);
        assertEquals(3, page.getTotal()); assertEquals(2, page.getRecords().size());
        assertTrue(page.getRecords().get(0).getId() > page.getRecords().get(1).getId());
        assertEquals(deposit.getId(), bank.transactions(a.getAccountNo(), 2, 2).getRecords().get(0).getId());
        assertTrue(bank.transactions(a.getAccountNo(), 3, 2).getRecords().isEmpty());
    }

    @ParameterizedTest @NullSource @ValueSource(strings={"0", "-0.01", "1.001", "10000000000000000.00"})
    void invalidMoneyChangesNeitherBalanceNorLedger(String value) throws Exception {
        var a = bank.openAccount("客户", "1");
        BigDecimal amount = value == null ? null : money(value);
        assertThrows(IllegalArgumentException.class, () -> bank.deposit(a.getAccountNo(), amount));
        assertThrows(IllegalArgumentException.class, () -> bank.withdraw(a.getAccountNo(), amount));
        assertEquals(money("0.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(0, count("bank_transaction"));
    }

    @Test void decimalBoundaryAndOverflowAreExact() {
        var a = bank.openAccount("客户", "1");
        bank.deposit(a.getAccountNo(), money("0.0100"));
        bank.deposit(a.getAccountNo(), BankValidation.MAX_BALANCE.subtract(money("0.01")));
        assertEquals(BankValidation.MAX_BALANCE, bank.account(a.getAccountNo()).getBalance());
        assertThrows(IllegalArgumentException.class, () -> bank.deposit(a.getAccountNo(), money("0.01")));
        bank.withdraw(a.getAccountNo(), BankValidation.MAX_BALANCE);
        assertEquals(money("0.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(3, bank.transactions(a.getAccountNo(), 1, 10).getTotal());
    }

    @Test void insufficientBalanceDoesNotWriteLedger() {
        var a = bank.openAccount("客户", "1"); bank.deposit(a.getAccountNo(), money("5"));
        assertThrows(IllegalArgumentException.class, () -> bank.withdraw(a.getAccountNo(), money("5.01")));
        assertEquals(money("5.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(1, bank.transactions(a.getAccountNo(), 1, 10).getTotal());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "missing-account"})
    void invalidOrMissingAccountIsRejected(String number) {
        assertThrows(IllegalArgumentException.class, () -> bank.account(number));
        assertThrows(IllegalArgumentException.class, () -> bank.deposit(number, money("1")));
        assertThrows(IllegalArgumentException.class, () -> bank.withdraw(number, money("1")));
        assertThrows(IllegalArgumentException.class, () -> bank.transactions(number, 1, 1));
    }

    @Test void invalidCustomerAndPageArgumentsAreRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> bank.openAccount((Integer)null));
        assertThrows(IllegalArgumentException.class, () -> bank.openAccount(0));
        assertThrows(IllegalArgumentException.class, () -> bank.openAccount(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> bank.openAccount("", "1"));
        assertThrows(IllegalArgumentException.class, () -> bank.addCustomer("客户", null));
        assertThrows(IllegalArgumentException.class, () -> bank.addCustomer("客".repeat(51), "1"));
        assertThrows(IllegalArgumentException.class, () -> bank.addCustomer("客户", "1".repeat(31)));
        var a = bank.openAccount("客户", "1");
        assertThrows(IllegalArgumentException.class, () -> bank.transactions(a.getAccountNo(), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> bank.transactions(a.getAccountNo(), 1, -1));
        assertEquals(1, count("bank_customer"));
    }

    @Test void failedLedgerInsertionRollsBackBalance() {
        var a = bank.openAccount("客户", "1");
        withFailure("com.example.mapper.BankTransactionMapper.insert", broken -> {
            var error = assertThrows(org.apache.ibatis.exceptions.PersistenceException.class,
                    () -> broken.deposit(a.getAccountNo(), money("10")));
            assertTrue(error.getMessage().contains("测试模拟数据库写入失败"));
        });
        assertEquals(money("0.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(0, bank.transactions(a.getAccountNo(), 1, 10).getTotal());
    }

    @Test void failedAccountInsertionRollsBackNewCustomer() throws Exception {
        withFailure("com.example.mapper.BankAccountMapper.insert", broken -> {
            var error = assertThrows(org.apache.ibatis.exceptions.PersistenceException.class,
                    () -> broken.openAccount("不应保留", "1"));
            assertTrue(error.getMessage().contains("测试模拟数据库写入失败"));
        });
        assertEquals(0, count("bank_customer")); assertEquals(0, count("bank_account"));
    }

    private void withFailure(String statement, java.util.function.Consumer<BankService> check) {
        var isolated = MyBatisUtil.createSqlSessionFactory(properties);
        isolated.getConfiguration().addInterceptor(new FailUpdate(statement));
        try { check.accept(new BankService(isolated)); }
        finally { ((PooledDataSource)isolated.getConfiguration().getEnvironment().getDataSource()).forceCloseAll(); }
    }

    @Intercepts(@Signature(type=Executor.class, method="update", args={MappedStatement.class, Object.class}))
    public static class FailUpdate implements Interceptor {
        private final String id;
        public FailUpdate(String id) { this.id = id; }
        public Object intercept(Invocation invocation) throws Throwable {
            if (((MappedStatement)invocation.getArgs()[0]).getId().equals(id))
                throw new IllegalStateException("测试模拟数据库写入失败");
            return invocation.proceed();
        }
    }

    @Test void databaseFailureInBatchInsertIsAtomic() throws Exception {
        execute("CREATE TRIGGER bank_test_reject BEFORE INSERT ON bank_customer FOR EACH ROW BEGIN IF NEW.name = '拒绝' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test rejection'; END IF; END");
        try {
            assertThrows(org.apache.ibatis.exceptions.PersistenceException.class, () -> bank.addCustomers(java.util.List.of(
                    new Customer(null, "有效", "1"), new Customer(null, "拒绝", "2"))));
            assertEquals(0, count("bank_customer"));
        } finally { execute("DROP TRIGGER bank_test_reject"); }
    }
}
