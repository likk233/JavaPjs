package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.List;

/** 课程实体，通过 student_course 中间表与学生构成多对多关系。 */
@TableName("course")
public class Course {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String courseNo;
    private String name;

    /** 选修该课程的学生，由关联查询填充，不是 course 表字段。 */
    @TableField(exist = false)
    private List<Student> students;

    public Course() {
    }

    public Course(Integer id, String courseNo, String name) {
        this.id = id;
        this.courseNo = courseNo;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCourseNo() {
        return courseNo;
    }

    public void setCourseNo(String courseNo) {
        this.courseNo = courseNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    @Override
    public String toString() {
        // 只输出基本字段，避免学生和课程双向引用时循环打印。
        return "Course{" +
                "id=" + id +
                ", courseNo='" + courseNo + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
