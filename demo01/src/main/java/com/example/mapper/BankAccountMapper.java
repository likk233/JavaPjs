package com.example.mapper;

import com.example.entity.BankAccount;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

/** 余额更新必须与加锁查询、流水插入处于同一个事务，参见 BankService。 */
public interface BankAccountMapper {
    int insert(BankAccount account);
    BankAccount selectByAccountNo(@Param("accountNo") String accountNo);
    BankAccount selectForUpdate(@Param("accountNo") String accountNo);
    List<BankAccount> selectByCustomerId(@Param("customerId") Integer customerId);
    int updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
