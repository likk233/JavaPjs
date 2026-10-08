package com.example.mapper;

import com.example.entity.Dep;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一对多关联测试（部门 -> 员工）。
 * 数据由 sql/emp_dep_init.sql 初始化，请先执行该脚本。
 */
public class DepMapperTest {

    @Test
    void testSelectById() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            DepMapper mapper = session.getMapper(DepMapper.class);
            Dep dep = mapper.selectById(1);

            assertNotNull(dep, "按主键查部门应有结果");
            assertEquals("研发部", dep.getDepName());
            assertEquals("北京", dep.getLoc());
            assertNotNull(dep.getEmps(), "一对多应带出员工集合");
            assertEquals(3, dep.getEmps().size(), "研发部应有 3 名员工");

            boolean found = dep.getEmps().stream().anyMatch(e -> "张伟".equals(e.getEmpName()));
            assertTrue(found, "研发部应包含张伟");
            System.out.println("一对多(嵌套结果) -> " + dep.getDepName() + " 员工: " + dep.getEmps());
        }
    }

    @Test
    void testSelectByIdNested() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            DepMapper mapper = session.getMapper(DepMapper.class);
            Dep dep = mapper.selectByIdNested(2);

            assertNotNull(dep);
            assertEquals("市场部", dep.getDepName());
            assertNotNull(dep.getEmps());
            assertEquals(2, dep.getEmps().size(), "市场部应有 2 名员工");
            System.out.println("一对多(嵌套 select) -> " + dep.getDepName() + " 员工: " + dep.getEmps());
        }
    }

    @Test
    void testSelectAllWithEmps() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            DepMapper mapper = session.getMapper(DepMapper.class);
            List<Dep> deps = mapper.selectAllWithEmps();

            assertNotNull(deps);
            assertEquals(4, deps.size(), "部门应为 4 个");
            Dep rnd = deps.stream()
                    .filter(d -> "研发部".equals(d.getDepName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("应能查到研发部"));
            assertEquals(3, rnd.getEmps().size(), "研发部应有 3 名员工");
        }
    }

    @Test
    void testSelectByIdBase() {
        try (SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession(true)) {
            DepMapper mapper = session.getMapper(DepMapper.class);
            Dep dep = mapper.selectByIdBase(1);

            assertNotNull(dep);
            assertEquals("研发部", dep.getDepName());
            assertNull(dep.getEmps(), "基础查询不应加载员工集合");
        }
    }
}
