package com.example.mapper;

import com.example.entity.Emp;
import java.util.List;

/**
 * 员工数据访问接口，演示多对一关联（Emp -> Dep）。
 */
public interface EmpMapper {

    /** 多对一（嵌套结果）：按主键查员工，LEFT JOIN 带出所属部门 */
    Emp selectById(Integer empId);

    /** 多对一（嵌套 select）：先查员工，再单独查部门 */
    Emp selectByIdNested(Integer empId);

    /** 多对一：查全部员工，每人带出所属部门 */
    List<Emp> selectAllWithDep();

    /** 供嵌套 select 使用：查询指定部门的全部员工（不带部门对象） */
    List<Emp> selectByDepId(Integer depId);
}
