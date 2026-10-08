package com.example.mapper;

import com.example.entity.Emp;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多对一关联测试（员工 -> 部门）。
 * 数据由 sql/emp_dep_init.sql 初始化，请先执行该脚本。
 */
public class EmpMapperTest {

    @Test
    void testSelectById() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            Emp emp = mapper.selectById(1);

            assertNotNull(emp, "按主键查员工应有结果");
            assertEquals("张伟", emp.getEmpName());
            assertEquals("Java工程师", emp.getJob());
            assertNotNull(emp.getDep(), "多对一应带出所属部门");
            assertEquals("研发部", emp.getDep().getDepName());
            assertEquals("北京", emp.getDep().getLoc());
            System.out.println("多对一(嵌套结果) -> " + emp.getEmpName() + " 属于 " + emp.getDep().getDepName());
        }
    }

    @Test
    void testSelectByIdNested() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            Emp emp = mapper.selectByIdNested(2);

            assertNotNull(emp, "按主键查员工应有结果");
            assertEquals("李娜", emp.getEmpName());
            assertNotNull(emp.getDep(), "嵌套 select 应带出所属部门");
            assertEquals("研发部", emp.getDep().getDepName());
            System.out.println("多对一(嵌套 select) -> " + emp.getEmpName() + " 属于 " + emp.getDep().getDepName());
        }
    }

    @Test
    void testSelectAllWithDep() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            List<Emp> emps = mapper.selectAllWithDep();

            assertNotNull(emps);
            assertEquals(7, emps.size(), "员工应为 7 名");
            for (Emp emp : emps) {
                assertNotNull(emp.getDep(), "每名员工都应带出部门");
            }
            boolean found = emps.stream()
                    .anyMatch(e -> "张伟".equals(e.getEmpName()) && "研发部".equals(e.getDep().getDepName()));
            assertTrue(found, "应能查到张伟属于研发部");
        }
    }

    @Test
    void testSelectByDepId() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            List<Emp> emps = mapper.selectByDepId(1);

            assertNotNull(emps);
            assertEquals(3, emps.size(), "研发部应有 3 名员工");
        }
    }
}
