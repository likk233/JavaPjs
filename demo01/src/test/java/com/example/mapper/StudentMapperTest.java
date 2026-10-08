package com.example.mapper;

import com.example.entity.Course;
import com.example.entity.Student;
import com.example.util.MyBatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学生、课程多对多关联测试，使用 db.properties 配置的真实 MySQL。
 * student、course、student_course 表需已存在并使用 InnoDB（见 sql/student_course_init.sql）。
 * 每个测试独立创建数据，结束时回滚，不删除已有记录，也不依赖固定主键或测试执行顺序。
 */
public class StudentMapperTest {

    private SqlSessionFactory factory;
    private SqlSession session;
    private StudentMapper studentMapper;
    private CourseMapper courseMapper;
    private SelectCounter selectCounter;
    private Student firstStudent;
    private Student secondStudent;
    private Student studentWithoutCourses;
    private Course firstCourse;
    private Course secondCourse;
    private Course courseWithoutStudents;

    @BeforeEach
    void setUp() throws SQLException {
        // 单独的工厂使 SQL 计数插件只作用于本测试，不影响应用和其他测试。
        factory = MyBatisUtil.createSqlSessionFactory(new Properties());
        selectCounter = new SelectCounter();
        factory.getConfiguration().addInterceptor(selectCounter);
        session = factory.openSession(false);
        studentMapper = session.getMapper(StudentMapper.class);
        courseMapper = session.getMapper(CourseMapper.class);

        // 编号最长 18 个字符，符合表中 VARCHAR(20) 的约束。
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        firstStudent = new Student(null, "S1" + suffix, "同名学生");
        secondStudent = new Student(null, "S2" + suffix, "同名学生");
        studentWithoutCourses = new Student(null, "S3" + suffix, "未选课学生");
        firstCourse = new Course(null, "C1" + suffix, "Java基础");
        secondCourse = new Course(null, "C2" + suffix, "MySQL数据库");
        courseWithoutStudents = new Course(null, "C3" + suffix, "未选修课程");

        assertEquals(1, studentMapper.insert(firstStudent));
        assertEquals(1, studentMapper.insert(secondStudent));
        assertEquals(1, studentMapper.insert(studentWithoutCourses));
        assertEquals(1, courseMapper.insert(firstCourse));
        assertEquals(1, courseMapper.insert(secondCourse));
        assertEquals(1, courseMapper.insert(courseWithoutStudents));
        for (Student student : List.of(firstStudent, secondStudent, studentWithoutCourses)) {
            assertNotNull(student.getId(), "学生自增主键应回填");
        }
        for (Course course : List.of(firstCourse, secondCourse, courseWithoutStudents)) {
            assertNotNull(course.getId(), "课程自增主键应回填");
        }

        // 第一名学生选两门课，第一门课由两名学生选修，构成真正的多对多关系。
        assertEquals(1, enroll(firstStudent.getId(), firstCourse.getId()));
        assertEquals(1, enroll(firstStudent.getId(), secondCourse.getId()));
        assertEquals(1, enroll(secondStudent.getId(), firstCourse.getId()));
        session.clearCache();
        selectCounter.reset();
    }

    @AfterEach
    void tearDown() {
        try {
            if (session != null) {
                try {
                    session.rollback();
                } finally {
                    session.close();
                }
            }
        } finally {
            if (factory != null) {
                ((PooledDataSource) factory.getConfiguration().getEnvironment()
                        .getDataSource()).forceCloseAll();
            }
        }
    }

    @Test
    void testSelectStudentWithCourses() {
        Student loaded = studentMapper.selectStudentWithCourses(firstStudent.getId());
        assertNotNull(loaded);
        assertEquals(firstStudent.getId(), loaded.getId());
        assertEquals(firstStudent.getStudentNo(), loaded.getStudentNo());
        assertEquals(firstStudent.getName(), loaded.getName());
        assertEquals(1, selectCounter.count(), "访问学生基本字段不加载课程");

        assertCourses(loaded.getCourses());
        assertEquals(2, selectCounter.count(), "首次访问课程才执行关联 SQL");
        assertCourses(loaded.getCourses());
        assertEquals(2, selectCounter.count(), "重复访问课程不重复查询");
    }

    @Test
    void testSelectCourseWithStudents() {
        Course loaded = courseMapper.selectCourseWithStudents(firstCourse.getId());
        assertNotNull(loaded);
        assertEquals(firstCourse.getId(), loaded.getId());
        assertEquals(firstCourse.getCourseNo(), loaded.getCourseNo());
        assertEquals(firstCourse.getName(), loaded.getName());
        assertEquals(1, selectCounter.count(), "访问课程基本字段不加载学生");

        assertStudents(loaded.getStudents());
        assertEquals(2, selectCounter.count(), "首次访问学生才执行关联 SQL");
        assertStudents(loaded.getStudents());
        assertEquals(2, selectCounter.count(), "重复访问学生不重复查询");
    }

    @Test
    void testSelectCoursesByStudentId() {
        assertCourses(courseMapper.selectCoursesByStudentId(firstStudent.getId()));
        List<Course> courses = courseMapper.selectCoursesByStudentId(secondStudent.getId());
        assertEquals(List.of(firstCourse.getId()), courses.stream().map(Course::getId).toList(),
                "同名学生的选课应按学生 ID 区分");
    }

    @Test
    void testSelectStudentsByCourseId() {
        assertStudents(studentMapper.selectStudentsByCourseId(firstCourse.getId()));
        List<Student> students = studentMapper.selectStudentsByCourseId(secondCourse.getId());
        assertEquals(List.of(firstStudent.getId()), students.stream().map(Student::getId).toList(),
                "不同课程的学生列表不能混淆");
    }

    @Test
    void testStudentWithoutCourses() {
        Student loaded = studentMapper.selectStudentWithCourses(studentWithoutCourses.getId());
        assertNotNull(loaded);
        assertEquals(studentWithoutCourses.getStudentNo(), loaded.getStudentNo());
        assertEquals(1, selectCounter.count());
        assertNotNull(loaded.getCourses());
        assertTrue(loaded.getCourses().isEmpty());
        assertEquals(2, selectCounter.count(), "空关联结果不应重复查询");
        assertTrue(courseMapper.selectCoursesByStudentId(studentWithoutCourses.getId()).isEmpty());
    }

    @Test
    void testCourseWithoutStudents() {
        Course loaded = courseMapper.selectCourseWithStudents(courseWithoutStudents.getId());
        assertNotNull(loaded);
        assertEquals(courseWithoutStudents.getCourseNo(), loaded.getCourseNo());
        assertEquals(1, selectCounter.count());
        assertNotNull(loaded.getStudents());
        assertTrue(loaded.getStudents().isEmpty());
        assertEquals(2, selectCounter.count(), "空关联结果不应重复查询");
        assertTrue(studentMapper.selectStudentsByCourseId(courseWithoutStudents.getId()).isEmpty());
    }

    @Test
    void testMissingAndNullIds() {
        // 当前表使用正整数自增主键；SQL 的等值条件传 null 时也不会匹配记录。
        for (Integer id : new Integer[]{-1, null}) {
            assertNull(studentMapper.selectStudentWithCourses(id));
            assertNull(courseMapper.selectCourseWithStudents(id));
            assertTrue(courseMapper.selectCoursesByStudentId(id).isEmpty());
            assertTrue(studentMapper.selectStudentsByCourseId(id).isEmpty());
        }
    }

    @Test
    void testDuplicateEnrollmentRejected() {
        SQLIntegrityConstraintViolationException exception = assertThrows(
                SQLIntegrityConstraintViolationException.class,
                () -> enroll(firstStudent.getId(), firstCourse.getId()));
        assertEquals(1062, exception.getErrorCode(), "联合主键应拒绝重复选课");
        assertEquals("23000", exception.getSQLState());
        assertCourses(courseMapper.selectCoursesByStudentId(firstStudent.getId()));
    }

    @Test
    void testEnrollmentWithMissingStudentRejected() {
        assertNull(studentMapper.selectById(-1), "异常路径需使用不存在的学生 ID");
        SQLIntegrityConstraintViolationException exception = assertThrows(
                SQLIntegrityConstraintViolationException.class, () -> enroll(-1, firstCourse.getId()));
        assertEquals(1452, exception.getErrorCode(), "学生外键应拒绝不存在的学生");
        assertEquals("23000", exception.getSQLState());
        assertStudents(studentMapper.selectStudentsByCourseId(firstCourse.getId()));
    }

    @Test
    void testEnrollmentWithMissingCourseRejected() {
        assertNull(courseMapper.selectById(-1), "异常路径需使用不存在的课程 ID");
        SQLIntegrityConstraintViolationException exception = assertThrows(
                SQLIntegrityConstraintViolationException.class, () -> enroll(firstStudent.getId(), -1));
        assertEquals(1452, exception.getErrorCode(), "课程外键应拒绝不存在的课程");
        assertEquals("23000", exception.getSQLState());
        assertCourses(courseMapper.selectCoursesByStudentId(firstStudent.getId()));
    }

    private int enroll(Integer studentId, Integer courseId) throws SQLException {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "INSERT INTO student_course (student_id, course_id) VALUES (?, ?)")) {
            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            return statement.executeUpdate();
        }
    }

    private void assertCourses(List<Course> courses) {
        assertNotNull(courses);
        assertEquals(List.of(firstCourse.getId(), secondCourse.getId()),
                courses.stream().map(Course::getId).toList(), "课程应完整、无重复并按 ID 排序");
        assertEquals(List.of(firstCourse.getCourseNo(), secondCourse.getCourseNo()),
                courses.stream().map(Course::getCourseNo).toList(), "course_no 应映射到 courseNo");
        assertEquals(List.of(firstCourse.getName(), secondCourse.getName()),
                courses.stream().map(Course::getName).toList());
        courses.forEach(course -> assertNull(course.getStudents(), "嵌套结果不递归加载学生"));
    }

    private void assertStudents(List<Student> students) {
        assertNotNull(students);
        assertEquals(List.of(firstStudent.getId(), secondStudent.getId()),
                students.stream().map(Student::getId).toList(), "同名学生不能被合并，结果按 ID 排序");
        assertEquals(List.of(firstStudent.getStudentNo(), secondStudent.getStudentNo()),
                students.stream().map(Student::getStudentNo).toList(), "student_no 应映射到 studentNo");
        assertEquals(List.of(firstStudent.getName(), secondStudent.getName()),
                students.stream().map(Student::getName).toList());
        students.forEach(student -> assertNull(student.getCourses(), "嵌套结果不递归加载课程"));
    }

    /** 统计实际 JDBC SELECT，避免一级缓存导致延迟加载测试误判。 */
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare",
            args = {Connection.class, Integer.class}))
    private static class SelectCounter implements Interceptor {
        private int count;

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            String sql = ((StatementHandler) invocation.getTarget()).getBoundSql().getSql();
            Object result = invocation.proceed();
            if (sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) {
                count++;
            }
            return result;
        }

        int count() {
            return count;
        }

        void reset() {
            count = 0;
        }
    }
}
