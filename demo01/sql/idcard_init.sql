-- 在 MySQL 中执行本脚本，创建「用户 - 身份证」一对一关联所需的表与初始数据。
-- 依赖：mybatis_demo 数据库已存在，且 user 表已存在（见 init.sql）。

USE mybatis_demo;

DROP TABLE IF EXISTS `id_card`;

-- 身份证表：user_id 唯一，一张身份证只属于一个用户（一对一）
CREATE TABLE `id_card` (
    `id`      INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `card_no` VARCHAR(30)  NOT NULL COMMENT '身份证号',
    `address` VARCHAR(100)          COMMENT '住址',
    `user_id` INT          NOT NULL COMMENT '所属用户外键（唯一，一对一）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idcard_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '身份证表';

-- 初始测试数据：对应 init.sql 中的三个用户
INSERT INTO `id_card` (`card_no`, `address`, `user_id`) VALUES
('110101199001011234', '北京市朝阳区', 1),
('310101199202022345', '上海市浦东新区', 2),
('440101199303033456', '广州市天河区', 3);
