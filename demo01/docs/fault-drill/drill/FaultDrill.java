import com.example.entity.BankAccount;
import com.example.entity.Customer;
import com.example.mapper.BankAccountMapper;
import com.example.mapper.CustomerMapper;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

/**
 * 实验任务 5.1 故障演练入口，配合 run-drills.sh 使用。
 * 本文件位于 docs/ 下，不参与 Maven 编译，也不会进入生产代码。
 *
 * 用法：java FaultDrill parameter|resultmap|generated-key
 */
public final class FaultDrill {
    private FaultDrill() {}

    public static void main(String[] args) {
        String mode = args.length == 0 ? "" : args[0];
        var factory = MyBatisUtil.getSqlSessionFactory();
        try (SqlSession session = factory.openSession(true)) {
            switch (mode) {
                case "parameter" -> parameter(session);
                case "resultmap" -> resultMap(session);
                case "generated-key" -> generatedKey(session);
                default -> throw new IllegalArgumentException("未知演练模式：" + mode);
            }
        }
    }

    /** 场景一：XML 占位符参数名与 @Param 名称不一致。 */
    private static void parameter(SqlSession session) {
        System.out.println("==> 调用 selectByAccountNo(\"BA-DRILL\")");
        BankAccount account = session.getMapper(BankAccountMapper.class).selectByAccountNo("BA-DRILL");
        System.out.println("查询结果：" + account);
    }

    /** 场景二：resultMap 的 column 写错，查询不报错但属性为 null。 */
    private static void resultMap(SqlSession session) {
        CustomerMapper customers = session.getMapper(CustomerMapper.class);
        Customer customer = new Customer(null, "映射演练", "100");
        customers.insert(customer);

        BankAccountMapper accounts = session.getMapper(BankAccountMapper.class);
        BankAccount account = new BankAccount();
        account.setAccountNo("BA-DRILL");
        account.setCustomerId(customer.getId());
        accounts.insert(account);

        BankAccount loaded = accounts.selectByAccountNo("BA-DRILL");
        System.out.println("查询结果：" + loaded);
        System.out.println("accountNo = " + loaded.getAccountNo());
        System.out.println("balance   = " + loaded.getBalance());
        System.out.println("createdAt = " + loaded.getCreatedAt());
    }

    /** 场景三：去掉 useGeneratedKeys/keyProperty 后主键不回填。 */
    private static void generatedKey(SqlSession session) {
        CustomerMapper mapper = session.getMapper(CustomerMapper.class);
        List<Customer> rows = List.of(new Customer(null, "主键演练", "100"));
        System.out.println("影响行数：" + mapper.insertRows(rows));
        System.out.println("回填主键：" + rows.get(0).getId());
    }
}
