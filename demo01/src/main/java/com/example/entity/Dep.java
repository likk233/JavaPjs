package com.example.entity;

import java.util.List;

/**
 * 部门实体，与员工构成「一对多」关系：一个部门有多个员工。
 */
public class Dep {

    private Integer depId;
    private String depName;
    private String loc;

    /** 一对多：该部门下的员工 */
    private List<Emp> emps;

    public Dep() {
    }

    public Integer getDepId() {
        return depId;
    }

    public void setDepId(Integer depId) {
        this.depId = depId;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public List<Emp> getEmps() {
        return emps;
    }

    public void setEmps(List<Emp> emps) {
        this.emps = emps;
    }

    @Override
    public String toString() {
        return "Dep{" +
                "depId=" + depId +
                ", depName='" + depName + '\'' +
                ", loc='" + loc + '\'' +
                '}';
    }
}
