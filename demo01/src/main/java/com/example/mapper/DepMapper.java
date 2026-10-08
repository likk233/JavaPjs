package com.example.mapper;

import com.example.entity.Dep;
import java.util.List;

/**
 * 部门数据访问接口，演示一对多关联（Dep -> Emp）。
 */
public interface DepMapper {

    /** 仅查部门本身（不含员工），供多对一的嵌套 select 使用 */
    Dep selectByIdBase(Integer depId);

    /** 一对多（嵌套结果）：按主键查部门，LEFT JOIN 带出全部员工 */
    Dep selectById(Integer depId);

    /** 一对多（嵌套 select）：先查部门，再单独查员工 */
    Dep selectByIdNested(Integer depId);

    /** 一对多：查全部部门，每个部门带出员工列表 */
    List<Dep> selectAllWithEmps();
}
