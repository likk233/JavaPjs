package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.List;

/** 学生实体，通过 student_course 中间表与课程构成多对多关系。 */
@TableName("student")
public class Student {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String studentNo;
    private String name;

    /** 该学生选修的课程，由关联查询填充，不是 student 表字段。 */
    @TableField(exist = false)
    private List<Course> courses;

    public Student() {
    }

    public Student(Integer id, String studentNo, String name) {
        this.id = id;
        this.studentNo = studentNo;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    @Override
    public String toString() {
        // 只输出基本字段，避免学生和课程双向引用时循环打印。
        return "Student{" +
                "id=" + id +
                ", studentNo='" + studentNo + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
