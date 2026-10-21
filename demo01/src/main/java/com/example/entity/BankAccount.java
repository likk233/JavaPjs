package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 银行业务实体；金额使用精确十进制。 */
@TableName("bank_account")
public class BankAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNo;
    private Integer customerId;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public BankAccount() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "BankAccount{" + "id=" + id + ", " + "accountNo=" + accountNo + ", " + "customerId=" + customerId + ", " + "balance=" + balance + ", " + "createdAt=" + createdAt + "}";
    }
}
