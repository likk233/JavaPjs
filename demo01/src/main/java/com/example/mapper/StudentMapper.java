package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Student;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.FetchType;

import java.util.List;

/** 学生 Mapper：通用 CRUD 和学生、课程的多对多关联查询。 */
public interface StudentMapper extends BaseMapper<Student> {

    /** 查询学生，访问 getCourses() 时加载该学生选修的课程。 */
    @Select("SELECT id, student_no, name FROM student WHERE id = #{id}")
    @Results(id = "studentWithCoursesMap", value = {
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "student_no", property = "studentNo"),
            @Result(column = "name", property = "name"),
            @Result(column = "id", property = "courses",
                    many = @Many(select = "com.example.mapper.CourseMapper.selectCoursesByStudentId",
                            fetchType = FetchType.LAZY))
    })
    Student selectStudentWithCourses(Integer id);

    /** 查询选修某门课程的学生基本信息，供 Course.students 的嵌套查询使用。 */
    @Select("""
            SELECT s.id, s.student_no, s.name
            FROM student s
            JOIN student_course sc ON sc.student_id = s.id
            WHERE sc.course_id = #{courseId}
            ORDER BY s.id
            """)
    List<Student> selectStudentsByCourseId(Integer courseId);
}
