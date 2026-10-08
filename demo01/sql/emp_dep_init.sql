-- 在 MySQL 中执行本脚本，创建「员工 - 部门」两张表与初始数据。
-- 用于一对多（dep -> emp）与多对一（emp -> dep）关联映射练习。
-- 数据为自定义中文数据，非经典 SCOTT 库的 emp（SMITH 开头）表。

USE mybatis_demo;

-- 先删员工表再删部门表
DROP TABLE IF EXISTS `emp`;
DROP TABLE IF EXISTS `dep`;

-- 部门表
CREATE TABLE `dep` (
    `dep_id`   INT         NOT NULL AUTO_INCREMENT COMMENT '部门编号',
    `dep_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
    `loc`      VARCHAR(50)          COMMENT '部门地点',
    PRIMARY KEY (`dep_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '部门表';

-- 员工表：dep_id 指向 dep.dep_id（多对一：一个部门有多个员工，一个员工属于一个部门）
CREATE TABLE `emp` (
    `emp_id`   INT           NOT NULL AUTO_INCREMENT COMMENT '员工编号',
    `emp_name` VARCHAR(50)   NOT NULL COMMENT '员工姓名',
    `job`      VARCHAR(50)            COMMENT '岗位',
    `salary`   DECIMAL(10,2)          COMMENT '工资',
    `dep_id`   INT                    COMMENT '所属部门编号',
    PRIMARY KEY (`emp_id`),
    KEY `idx_emp_dep` (`dep_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '员工表';

-- 初始数据：4 个部门
INSERT INTO `dep` (`dep_name`, `loc`) VALUES
('研发部', '北京'),
('市场部', '上海'),
('财务部', '广州'),
('人事部', '深圳');

-- 初始数据：7 名员工（研发部 3 人、市场部 2 人、财务部 1 人、人事部 1 人）
INSERT INTO `emp` (`emp_name`, `job`, `salary`, `dep_id`) VALUES
('张伟', 'Java工程师',  12000.00, 1),
('李娜', '前端工程师',  11000.00, 1),
('赵磊', '测试工程师',   9500.00, 1),
('王强', '市场专员',     8000.00, 2),
('陈静', '市场经理',    15000.00, 2),
('刘洋', '会计',         9000.00, 3),
('孙丽', '人事专员',     7000.00, 4);
