package com.example.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.BankTransaction;
import org.apache.ibatis.annotations.Param;

public interface BankTransactionMapper {
    int insert(BankTransaction transaction);
    BankTransaction selectById(@Param("id") Long id);
    Page<BankTransaction> selectAccountPage(@Param("page") Page<BankTransaction> page,
                                          @Param("accountId") Long accountId);
}
