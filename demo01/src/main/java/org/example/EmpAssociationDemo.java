package org.example;

import com.example.entity.Dep;
import com.example.entity.Emp;
import com.example.mapper.EmpMapper;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * 使用 sql/emp_dep_init.sql 中的自定义中文员工数据，默认读取 db.properties。
 * 可将 JDBC URL 作为程序参数，用户名和密码仍取自 db.properties。
 * 标题沿用题目；员工与部门的实际关系为多对一，前两段演示单个关联对象的映射。
 */
public final class EmpAssociationDemo {

    private EmpAssociationDemo() {
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("用法：EmpAssociationDemo [JDBC URL]");
        }
        Properties overrides = new Properties();
        if (args.length == 1) {
            if (args[0].isBlank()) {
                throw new IllegalArgumentException("JDBC URL 不能为空");
            }
            overrides.setProperty("jdbc.url", args[0]);
        }
        SqlSessionFactory factory = MyBatisUtil.createSqlSessionFactory(overrides);
        try (SqlSession session = factory.openSession()) {
            printDemo(session.getMapper(EmpMapper.class), System.out);
        } finally {
            ((PooledDataSource) factory.getConfiguration().getEnvironment()
                    .getDataSource()).forceCloseAll();
        }
    }

    public static void printDemo(EmpMapper mapper, PrintStream out) {
        Objects.requireNonNull(mapper, "mapper 不能为空");
        Objects.requireNonNull(out, "out 不能为空");

        out.println("========== 一对一（XML方式）==========");
        out.println("知识点：<association> + 嵌套结果映射");
        printEmployee(mapper.selectById(1), 1, out);

        out.println("========== 一对一（注解方式）==========");
        out.println("知识点：@Results + @One(select = ...) 嵌套 select");
        printEmployee(mapper.selectByIdAnnotation(2), 2, out);

        out.println("========== 多对一（XML方式）==========");
        List<Emp> emps = mapper.selectAllWithDep();
        out.println("员工总数：" + emps.size());
        for (Emp emp : emps.stream().limit(3).toList()) {
            Dep dep = emp.getDep();
            out.printf("  %d - %s -> %s%n", emp.getEmpId(), emp.getEmpName(),
                    dep == null ? "未分配部门" : dep.getDepName());
        }
    }

    private static void printEmployee(Emp emp, int empId, PrintStream out) {
        if (emp == null) {
            out.println("未找到员工（编号:" + empId + "），请检查数据库及初始化脚本");
            return;
        }
        out.printf("员工：%s (编号:%d)%n", emp.getEmpName(), emp.getEmpId());
        Dep dep = emp.getDep();
        out.println("部门：" + (dep == null ? "未分配部门" : dep.getDepName()));
        out.println("位置：" + (dep == null || dep.getLoc() == null ? "未知" : dep.getLoc()));
    }
}
