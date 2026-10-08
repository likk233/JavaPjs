package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Course;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.FetchType;

import java.util.List;

/** 课程 Mapper：通用 CRUD 和课程、学生的多对多关联查询。 */
public interface CourseMapper extends BaseMapper<Course> {

    /** 查询课程，访问 getStudents() 时加载选修该课程的学生。 */
    @Select("SELECT id, course_no, name FROM course WHERE id = #{id}")
    @Results(id = "courseWithStudentsMap", value = {
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "course_no", property = "courseNo"),
            @Result(column = "name", property = "name"),
            @Result(column = "id", property = "students",
                    many = @Many(select = "com.example.mapper.StudentMapper.selectStudentsByCourseId",
                            fetchType = FetchType.LAZY))
    })
    Course selectCourseWithStudents(Integer id);

    /** 查询某个学生选修的课程基本信息，供 Student.courses 的嵌套查询使用。 */
    @Select("""
            SELECT c.id, c.course_no, c.name
            FROM course c
            JOIN student_course sc ON sc.course_id = c.id
            WHERE sc.student_id = #{studentId}
            ORDER BY c.id
            """)
    List<Course> selectCoursesByStudentId(Integer studentId);
}
