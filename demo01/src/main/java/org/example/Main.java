package org.example;

import com.example.service.BankService;
import com.example.util.BankSchemaValidator;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 银行启动入口；首次运行先执行 sql/bank_init.sql。 */
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        var factory = MyBatisUtil.getSqlSessionFactory();
        try {
            BankSchemaValidator.validate(factory);
            new BankConsole(new BankService(factory),
                    new InputStreamReader(System.in, StandardCharsets.UTF_8), System.out).run();
        } finally {
            ((PooledDataSource) factory.getConfiguration().getEnvironment().getDataSource()).forceCloseAll();
        }
    }
}
