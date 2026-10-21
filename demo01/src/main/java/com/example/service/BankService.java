package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.util.BankValidation;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/** 每个操作独立使用会话；仅余额与流水一起成功时提交。 */
public final class BankService {
    private final SqlSessionFactory factory;

    public BankService() { this(MyBatisUtil.getSqlSessionFactory()); }

    /** 允许测试注入独立数据库工厂，不改变应用共享工厂。 */
    public BankService(SqlSessionFactory factory) { this.factory = Objects.requireNonNull(factory); }

    private <T> T transaction(Function<SqlSession, T> action) {
        try (SqlSession session = factory.openSession(false)) {
            try {
                T result = action.apply(session);
                session.commit();
                return result;
            } catch (RuntimeException | Error e) {
                try { session.rollback(); } catch (RuntimeException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    public Customer addCustomer(String name, String phone) {
        Customer customer = customer(name, phone);
        return transaction(s -> { s.getMapper(CustomerMapper.class).insert(customer); return customer; });
    }

    private static Customer customer(String name, String phone) {
        return new Customer(null, BankValidation.text(name, "姓名", 50),
                BankValidation.text(phone, "电话", 30));
    }

    public int updateCustomer(Customer customer) {
        return transaction(s -> s.getMapper(CustomerMapper.class).updateSelective(customer));
    }

    public int addCustomers(List<Customer> customers) {
        return transaction(s -> s.getMapper(CustomerMapper.class).insertBatch(customers));
    }

    public int deleteCustomers(List<Integer> ids) {
        return transaction(s -> s.getMapper(CustomerMapper.class).deleteBatch(ids));
    }

    public List<Customer> findCustomers(Customer condition) {
        return transaction(s -> s.getMapper(CustomerMapper.class).selectByCondition(condition));
    }

    public List<Customer> findCustomersByPriority(String name, String phone) {
        return transaction(s -> s.getMapper(CustomerMapper.class).selectByPriority(name, phone));
    }

    public Page<Customer> customerPage(long current, long size, String name, String phone) {
        BankValidation.page(current, size);
        return transaction(s -> s.getMapper(CustomerMapper.class).selectCustomerPage(current, size, name, phone));
    }

    public BankAccount openAccount(Integer customerId) {
        BankValidation.id(customerId);
        return transaction(s -> {
            if (s.getMapper(CustomerMapper.class).selectById(customerId) == null)
                throw new IllegalArgumentException("客户不存在");
            return createAccount(s, customerId);
        });
    }

    public BankAccount openAccount(String name, String phone) {
        Customer customer = customer(name, phone);
        return transaction(s -> {
            s.getMapper(CustomerMapper.class).insert(customer);
            return createAccount(s, customer.getId());
        });
    }

    private BankAccount createAccount(SqlSession session, Integer customerId) {
        BankAccount account = new BankAccount();
        account.setAccountNo("BA" + UUID.randomUUID().toString().replace("-", ""));
        account.setCustomerId(customerId);
        account.setBalance(new BigDecimal("0.00"));
        BankAccountMapper mapper = session.getMapper(BankAccountMapper.class);
        mapper.insert(account);
        return mapper.selectByAccountNo(account.getAccountNo());
    }

    public BankAccount account(String accountNo) {
        String number = BankValidation.text(accountNo, "账号", 34);
        return transaction(s -> requireAccount(s.getMapper(BankAccountMapper.class).selectByAccountNo(number)));
    }

    public List<BankAccount> accounts(Integer customerId) {
        BankValidation.id(customerId);
        return transaction(s -> s.getMapper(BankAccountMapper.class).selectByCustomerId(customerId));
    }

    public BankTransaction deposit(String accountNo, BigDecimal amount) {
        return changeBalance(accountNo, amount, TransactionType.DEPOSIT);
    }

    public BankTransaction withdraw(String accountNo, BigDecimal amount) {
        return changeBalance(accountNo, amount, TransactionType.WITHDRAW);
    }

    private BankTransaction changeBalance(String accountNo, BigDecimal amount, TransactionType type) {
        String number = BankValidation.text(accountNo, "账号", 34);
        BigDecimal money = BankValidation.amount(amount);
        return transaction(s -> {
            BankAccountMapper mapper = s.getMapper(BankAccountMapper.class);
            BankAccount account = requireAccount(mapper.selectForUpdate(number));
            BigDecimal before = account.getBalance();
            BigDecimal after = type == TransactionType.DEPOSIT ? before.add(money) : before.subtract(money);
            if (after.signum() < 0) throw new IllegalArgumentException("余额不足");
            if (after.compareTo(BankValidation.MAX_BALANCE) > 0)
                throw new IllegalArgumentException("存款后余额超出允许范围");
            if (mapper.updateBalance(account.getId(), after) != 1)
                throw new IllegalStateException("余额更新失败");
            BankTransaction entry = new BankTransaction();
            entry.setAccountId(account.getId()); entry.setType(type); entry.setAmount(money);
            entry.setBalanceBefore(before); entry.setBalanceAfter(after);
            if (s.getMapper(BankTransactionMapper.class).insert(entry) != 1)
                throw new IllegalStateException("流水写入失败");
            // 从数据库读取时间和金额，返回完整且已经映射的流水。
            return s.getMapper(BankTransactionMapper.class).selectById(entry.getId());
        });
    }

    private static BankAccount requireAccount(BankAccount account) {
        if (account == null) throw new IllegalArgumentException("账户不存在");
        return account;
    }

    public Page<BankTransaction> transactions(String accountNo, long current, long size) {
        String number = BankValidation.text(accountNo, "账号", 34);
        BankValidation.page(current, size);
        return transaction(s -> {
            BankAccount account = requireAccount(s.getMapper(BankAccountMapper.class).selectByAccountNo(number));
            return s.getMapper(BankTransactionMapper.class).selectAccountPage(new Page<>(current, size), account.getId());
        });
    }
}
