package com.example.mapper;

import com.example.entity.Emp;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.EmpAssociationDemo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 MySQL 测试：将示例脚本加载到连接专属的临时表，遮蔽同名持久表。
 * 不创建数据库，不修改已有数据；关闭连接后临时表自动释放。
 */
public class EmpAssociationDemoTest {

    private SqlSessionFactory factory;
    private SqlSession session;
    private EmpMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        factory = MyBatisUtil.createSqlSessionFactory(new Properties());
        session = factory.openSession(false);
        mapper = session.getMapper(EmpMapper.class);
        String script = Files.readString(Path.of("sql", "emp_dep_init.sql"));
        // 此受控脚本不包含字符串内的分号，去掉行注释后按分号执行即可。
        script = script.replaceAll("(?m)^--.*$", "");
        for (String part : script.split(";")) {
            String sql = part.strip();
            // 初始化脚本中的 USE 和 DROP 不在测试执行，避免触及已有持久表。
            if (sql.isEmpty() || sql.startsWith("USE ") || sql.startsWith("DROP TABLE")) {
                continue;
            }
            execute(sql.replace("CREATE TABLE", "CREATE TEMPORARY TABLE"));
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (session != null) {
                try {
                    session.rollback();
                } finally {
                    session.close();
                }
            }
        } finally {
            if (factory != null) {
                ((PooledDataSource) factory.getConfiguration().getEnvironment()
                        .getDataSource()).forceCloseAll();
            }
        }
    }

    @Test
    void printsRequestedOutput() {
        assertEquals("""
                ========== 一对一（XML方式）==========
                知识点：<association> + 嵌套结果映射
                员工：张伟 (编号:1)
                部门：研发部
                位置：北京
                ========== 一对一（注解方式）==========
                知识点：@Results + @One(select = ...) 嵌套 select
                员工：李娜 (编号:2)
                部门：研发部
                位置：北京
                ========== 多对一（XML方式）==========
                员工总数：7
                  1 - 张伟 -> 研发部
                  2 - 李娜 -> 研发部
                  3 - 赵磊 -> 研发部
                """, output());
    }

    @Test
    void annotationMapsEmployeeAndDepartmentFields() {
        Emp emp = mapper.selectByIdAnnotation(2);
        assertNotNull(emp);
        assertEquals(2, emp.getEmpId());
        assertEquals("李娜", emp.getEmpName());
        assertEquals("前端工程师", emp.getJob());
        assertEquals(11000.0, emp.getSalary());
        assertEquals(1, emp.getDepId());
        assertNotNull(emp.getDep());
        assertEquals(1, emp.getDep().getDepId());
        assertEquals("研发部", emp.getDep().getDepName());
        assertEquals("北京", emp.getDep().getLoc());
        assertNull(emp.getDep().getEmps(), "只加载部门本身，避免循环查询员工");
    }

    @Test
    void missingAndNullIdsReturnNoEmployee() {
        for (Integer id : new Integer[]{-1, null, Integer.MAX_VALUE}) {
            assertNull(mapper.selectById(id));
            assertNull(mapper.selectByIdAnnotation(id));
        }
    }

    @Test
    void nullAndMissingDepartmentsAreHandled() throws Exception {
        execute("UPDATE emp SET dep_id = NULL WHERE emp_id = 1");
        execute("UPDATE emp SET dep_id = 999 WHERE emp_id = 2");
        for (int id : new int[]{1, 2}) {
            assertNull(mapper.selectById(id).getDep());
            assertNull(mapper.selectByIdAnnotation(id).getDep());
        }
        String text = output();
        assertTrue(text.contains("部门：未分配部门\n位置：未知"));
        assertTrue(text.contains("  1 - 张伟 -> 未分配部门"));
        assertTrue(text.contains("  2 - 李娜 -> 未分配部门"));
    }

    @Test
    void emptyAndShortListsAreHandled() throws Exception {
        execute("DELETE FROM emp WHERE emp_id <> 1");
        String shortOutput = output();
        assertTrue(shortOutput.contains("员工总数：1\n  1 - 张伟 -> 研发部\n"));
        assertTrue(shortOutput.contains("未找到员工（编号:2）"));

        execute("DELETE FROM emp");
        session.clearCache();
        String emptyOutput = output();
        assertTrue(emptyOutput.contains("未找到员工（编号:1）"));
        assertTrue(emptyOutput.endsWith("员工总数：0\n"));
    }

    @Test
    void sharedDepartmentsAndDuplicateNamesDoNotMergeEmployees() throws Exception {
        execute("UPDATE emp SET emp_name = '李娜' WHERE emp_id = 3");
        List<Emp> emps = mapper.selectAllWithDep();
        assertEquals(7, emps.size());
        assertEquals(List.of(1, 2, 3),
                emps.stream().limit(3).map(Emp::getEmpId).toList());
        assertEquals("李娜", emps.get(1).getEmpName());
        assertEquals("李娜", emps.get(2).getEmpName());
        assertEquals("研发部", emps.get(1).getDep().getDepName());
        assertEquals("研发部", emps.get(2).getDep().getDepName());
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> EmpAssociationDemo.printDemo(null, System.out));
        assertThrows(NullPointerException.class, () -> EmpAssociationDemo.printDemo(mapper, null));
        assertThrows(IllegalArgumentException.class, () -> EmpAssociationDemo.main(new String[]{" "}));
        assertThrows(IllegalArgumentException.class, () -> EmpAssociationDemo.main(new String[]{"a", "b"}));
    }

    private void execute(String sql) throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute(sql);
        }
    }

    private String output() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            EmpAssociationDemo.printDemo(mapper, out);
        }
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
