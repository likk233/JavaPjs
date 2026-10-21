-- 在 db.properties 指定的数据库内执行；脚本不创建数据库、不删除表、不写入示例资金。
-- mysql -h localhost -u root -p mybatis_demo < sql/bank_init.sql
-- 仅创建缺失表；现有同名表结构若不兼容，程序启动时会明确拒绝运行。
CREATE TABLE IF NOT EXISTS bank_customer (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_no VARCHAR(34) NOT NULL,
    customer_id INT NOT NULL,
    balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_account_no (account_no),
    KEY idx_bank_account_customer (customer_id),
    CONSTRAINT fk_bank_account_customer FOREIGN KEY (customer_id)
        REFERENCES bank_customer(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_bank_account_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    type VARCHAR(8) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    balance_before DECIMAL(18,2) NOT NULL,
    balance_after DECIMAL(18,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_bank_transaction_account (account_id, id),
    CONSTRAINT fk_bank_transaction_account FOREIGN KEY (account_id)
        REFERENCES bank_account(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_bank_transaction CHECK (
        amount > 0 AND balance_before >= 0 AND balance_after >= 0 AND
        ((type = 'DEPOSIT' AND balance_after = balance_before + amount) OR
         (type = 'WITHDRAW' AND balance_after = balance_before - amount))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
