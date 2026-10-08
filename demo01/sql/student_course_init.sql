-- 学生与课程多对多关联：一个学生可以选多门课，一门课可以由多个学生选修。
-- 在 db.properties 配置的 mybatis_demo 数据库中执行。
-- 可重复执行：不删除已有表或数据，仅补充缺失的示例记录。

USE mybatis_demo;

-- 学生表：学号唯一，姓名允许重复。
CREATE TABLE IF NOT EXISTS `student` (
    `id`         INT         NOT NULL AUTO_INCREMENT COMMENT '学生主键',
    `student_no` VARCHAR(20) NOT NULL COMMENT '学号',
    `name`       VARCHAR(50) NOT NULL COMMENT '学生姓名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '学生表';

-- 课程表：课程编号唯一。
CREATE TABLE IF NOT EXISTS `course` (
    `id`        INT         NOT NULL AUTO_INCREMENT COMMENT '课程主键',
    `course_no` VARCHAR(20) NOT NULL COMMENT '课程编号',
    `name`      VARCHAR(50) NOT NULL COMMENT '课程名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_no` (`course_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '课程表';

-- 中间表：联合主键禁止重复选课，外键保证学生和课程都真实存在。
CREATE TABLE IF NOT EXISTS `student_course` (
    `student_id` INT NOT NULL COMMENT '学生外键',
    `course_id`  INT NOT NULL COMMENT '课程外键',
    PRIMARY KEY (`student_id`, `course_id`),
    KEY `idx_student_course_course` (`course_id`),
    CONSTRAINT `fk_student_course_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_student_course_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '学生-课程选课关联表';

START TRANSACTION;

INSERT INTO `student` (`student_no`, `name`)
SELECT seed.student_no, seed.name
FROM (
    SELECT 'S001' AS student_no, '张三' AS name
    UNION ALL SELECT 'S002', '李四'
    UNION ALL SELECT 'S003', '王五'
    UNION ALL SELECT 'S004', '赵六'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM `student` s WHERE s.student_no = seed.student_no
);

INSERT INTO `course` (`course_no`, `name`)
SELECT seed.course_no, seed.name
FROM (
    SELECT 'C001' AS course_no, 'Java基础' AS name
    UNION ALL SELECT 'C002', 'MySQL数据库'
    UNION ALL SELECT 'C003', 'MyBatis框架'
    UNION ALL SELECT 'C004', 'Spring框架'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM `course` c WHERE c.course_no = seed.course_no
);

-- 张三选 3 门课、李四选 2 门课、王五选 1 门课；赵六尚未选课。
-- Java基础有 2 名学生，MySQL数据库有 3 名学生，MyBatis框架有 1 名学生。
-- Spring框架尚未有学生选修，便于测试空关联集合。
INSERT INTO `student_course` (`student_id`, `course_id`)
SELECT s.id, c.id
FROM (
    SELECT 'S001' AS student_no, 'C001' AS course_no
    UNION ALL SELECT 'S001', 'C002'
    UNION ALL SELECT 'S001', 'C003'
    UNION ALL SELECT 'S002', 'C001'
    UNION ALL SELECT 'S002', 'C002'
    UNION ALL SELECT 'S003', 'C002'
) AS seed
JOIN `student` s ON s.student_no = seed.student_no
JOIN `course` c ON c.course_no = seed.course_no
WHERE NOT EXISTS (
    SELECT 1 FROM `student_course` sc WHERE sc.student_id = s.id AND sc.course_id = c.id
);

COMMIT;

-- 查询某个学生选修的课程（可用于后续 StudentMapper 关联查询）。
-- SELECT c.id, c.course_no, c.name
-- FROM student s
-- JOIN student_course sc ON sc.student_id = s.id
-- JOIN course c ON c.id = sc.course_id
-- WHERE s.student_no = 'S001'
-- ORDER BY c.id;

-- 查询某门课程的学生（可用于后续 CourseMapper 关联查询）。
-- SELECT s.id, s.student_no, s.name
-- FROM course c
-- JOIN student_course sc ON sc.course_id = c.id
-- JOIN student s ON s.id = sc.student_id
-- WHERE c.course_no = 'C002'
-- ORDER BY s.id;
