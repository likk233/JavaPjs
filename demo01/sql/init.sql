-- 在 MySQL 中执行本脚本，创建测试所需的数据库和表。
-- 数据库名需与 src/main/resources/db.properties 中 jdbc.url 保持一致（默认 mybatis_demo）。

CREATE DATABASE IF NOT EXISTS mybatis_demo
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE mybatis_demo;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id`       INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `email`    VARCHAR(100)          COMMENT '邮箱',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '用户表';

-- 插入初始测试数据
INSERT INTO `user` (`username`, `password`, `email`) VALUES
('张三', '123', 'zhangsan@qq.com'),
('李四', '456', 'lisi@qq.com'),
('王五', '789', 'wangwu@qq.com');
