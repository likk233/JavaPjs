package com.example.mapper;

import com.example.entity.Emp;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 员工数据访问接口，演示多对一关联（Emp -> Dep）。
 */
public interface EmpMapper {

    /** 多对一（嵌套结果）：按主键查员工，LEFT JOIN 带出所属部门 */
    Emp selectById(Integer empId);

    /** 多对一（嵌套 select）：先查员工，再单独查部门 */
    Emp selectByIdNested(Integer empId);

    /** 注解嵌套 select：先查员工，再通过部门编号查部门。 */
    @Select("SELECT emp_id, emp_name, job, salary, dep_id FROM emp WHERE emp_id = #{empId}")
    @Results(id = "empWithDepAnnotationMap", value = {
            @Result(id = true, property = "empId", column = "emp_id"),
            @Result(property = "empName", column = "emp_name"),
            @Result(property = "job", column = "job"),
            @Result(property = "salary", column = "salary"),
            @Result(property = "depId", column = "dep_id"),
            @Result(property = "dep", column = "dep_id",
                    one = @One(select = "com.example.mapper.DepMapper.selectByIdBase"))
    })
    Emp selectByIdAnnotation(@Param("empId") Integer empId);

    /** 多对一：查全部员工，每人带出所属部门 */
    List<Emp> selectAllWithDep();

    /** 供嵌套 select 使用：查询指定部门的全部员工（不带部门对象） */
    List<Emp> selectByDepId(Integer depId);
}
