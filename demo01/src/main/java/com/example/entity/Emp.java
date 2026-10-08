package com.example.entity;

/**
 * 员工实体，与部门构成「多对一」关系：一个员工属于一个部门。
 */
public class Emp {

    private Integer empId;
    private String empName;
    private String job;
    private Double salary;
    /** 外键：所属部门编号 */
    private Integer depId;

    /** 多对一：所属部门 */
    private Dep dep;

    public Emp() {
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public Integer getDepId() {
        return depId;
    }

    public void setDepId(Integer depId) {
        this.depId = depId;
    }

    public Dep getDep() {
        return dep;
    }

    public void setDep(Dep dep) {
        this.dep = dep;
    }

    @Override
    public String toString() {
        return "Emp{" +
                "empId=" + empId +
                ", empName='" + empName + '\'' +
                ", job='" + job + '\'' +
                ", salary=" + salary +
                ", depId=" + depId +
                '}';
    }
}
