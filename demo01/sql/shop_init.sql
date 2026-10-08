-- 在 MySQL 中执行本脚本，创建「供应商 - 商品 - 客户」三实体多表联查所需的表与初始数据。
-- 依赖：mybatis_demo 数据库已存在（见 init.sql）。

USE mybatis_demo;

-- ============ 建表（先删中间表再删主表，避免外键/顺序问题） ============
DROP TABLE IF EXISTS `supplier_customer`;
DROP TABLE IF EXISTS `supplier_product`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `customer`;
DROP TABLE IF EXISTS `supplier`;

-- 供应商表
CREATE TABLE `supplier` (
    `id`      INT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`    VARCHAR(50) NOT NULL COMMENT '供应商名称',
    `contact` VARCHAR(50)          COMMENT '联系方式',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '供应商表';

-- 商品表：customer_id 表示买走该商品的客户（一对多：一个客户买多个商品，一个商品只卖给一个客户）
CREATE TABLE `product` (
    `id`          INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)   NOT NULL COMMENT '商品名称',
    `price`       DECIMAL(10,2)          COMMENT '价格',
    `customer_id` INT                    COMMENT '买走该商品的客户，NULL 表示未售出',
    PRIMARY KEY (`id`),
    KEY `idx_product_customer` (`customer_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '商品表';

-- 客户表
CREATE TABLE `customer` (
    `id`    INT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`  VARCHAR(50) NOT NULL COMMENT '客户名称',
    `phone` VARCHAR(20)          COMMENT '电话',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '客户表';

-- 多对多中间表：供应商 - 商品
CREATE TABLE `supplier_product` (
    `supplier_id` INT NOT NULL COMMENT '供应商外键',
    `product_id`  INT NOT NULL COMMENT '商品外键',
    PRIMARY KEY (`supplier_id`, `product_id`),
    KEY `idx_sp_product` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '供应商-商品中间表';

-- 多对多中间表：供应商 - 客户（供应商给客户提供服务）
CREATE TABLE `supplier_customer` (
    `supplier_id` INT NOT NULL COMMENT '供应商外键',
    `customer_id` INT NOT NULL COMMENT '客户外键',
    PRIMARY KEY (`supplier_id`, `customer_id`),
    KEY `idx_sc_customer` (`customer_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '供应商-客户中间表（提供服务）';

-- ============ 初始数据 ============
INSERT INTO `supplier` (`name`, `contact`) VALUES
('华为', '400-800-1001'),
('小米', '400-800-1002'),
('苹果', '400-800-1003');

INSERT INTO `customer` (`name`, `phone`) VALUES
('张三', '13800000001'),
('李四', '13800000002'),
('王五', '13800000003'),
('赵六', '13800000004');

-- 商品：耳机未售出（customer_id 为 NULL），其余均已售出
INSERT INTO `product` (`name`, `price`, `customer_id`) VALUES
('手机',   5999.00, 1),
('平板',   2999.00, 2),
('笔记本', 6999.00, 1),
('耳机',    999.00, NULL),
('手表',   1299.00, 3);

-- 供应商-商品（多对多）
INSERT INTO `supplier_product` (`supplier_id`, `product_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),        -- 华为供应：手机、平板、笔记本、耳机
(2, 1), (2, 2), (2, 5),                 -- 小米供应：手机、平板、手表
(3, 1), (3, 3), (3, 4);                 -- 苹果供应：手机、笔记本、耳机

-- 供应商-客户（多对多：提供服务）
INSERT INTO `supplier_customer` (`supplier_id`, `customer_id`) VALUES
(1, 1), (1, 2),                         -- 华为服务：张三、李四
(2, 1), (2, 3),                         -- 小米服务：张三、王五
(3, 2), (3, 4);                         -- 苹果服务：李四、赵六
