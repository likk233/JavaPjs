package com.example.bank;

import com.example.util.*;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.log4j.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BankInfrastructureTest extends BankTestSupport {
    @Test void schemaAndPackageScanningAreValid() {
        assertDoesNotThrow(() -> BankSchemaValidator.validate(factory));
        assertTrue(factory.getConfiguration().hasMapper(com.example.mapper.CustomerMapper.class));
        assertTrue(factory.getConfiguration().hasMapper(com.example.mapper.BankAccountMapper.class));
        assertTrue(factory.getConfiguration().hasMapper(com.example.mapper.BankTransactionMapper.class));
        assertSame(MyBatisUtil.getSqlSessionFactory(), MyBatisUtil.getSqlSessionFactory());
    }

    @Test void mapperScanSkipsHiddenMetadata() throws Exception {
        var resources = new VisibleFilesVfs().list("com/example/mapper");
        assertTrue(resources.stream().anyMatch(name -> name.endsWith("CustomerMapper.class")));
        assertTrue(resources.stream().noneMatch(name -> java.util.Arrays.stream(name.split("/")).anyMatch(part -> part.startsWith("."))));
    }

    @Test void foreignKeysAndMoneyChecksProtectStoredData() throws Exception {
        var a = bank.openAccount("客户", "1");
        bank.deposit(a.getAccountNo(), new java.math.BigDecimal("1.00"));
        assertThrows(java.sql.SQLException.class, () -> execute("DELETE FROM bank_account WHERE id=" + a.getId()));
        assertThrows(java.sql.SQLException.class, () -> execute("UPDATE bank_account SET balance=-1 WHERE id=" + a.getId()));
        assertThrows(java.sql.SQLException.class, () -> execute("INSERT INTO bank_transaction(account_id,type,amount,balance_before,balance_after) VALUES (" + a.getId() + ",'DEPOSIT',1,1,99)"));
        assertEquals(new java.math.BigDecimal("1.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(1, count("bank_transaction"));
    }

    @Test void initializationIsRepeatableAndDoesNotEraseData() throws Exception {
        var a = bank.openAccount("不应被清空", "1");
        bank.deposit(a.getAccountNo(), new java.math.BigDecimal("12.34"));
        try (var session = factory.openSession(false); Reader script = Files.newBufferedReader(Path.of("sql/bank_init.sql"))) {
            ScriptRunner runner = new ScriptRunner(session.getConnection()); runner.setLogWriter(null); runner.setStopOnError(true);
            runner.runScript(script);
        }
        assertEquals(new java.math.BigDecimal("12.34"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(1, count("bank_transaction"));
    }

    @Test void incompatibleStructureIsRejected() throws Exception {
        execute("ALTER TABLE bank_customer MODIFY phone VARCHAR(31) NOT NULL");
        try { assertThrows(IllegalStateException.class, () -> BankSchemaValidator.validate(factory)); }
        finally { execute("ALTER TABLE bank_customer MODIFY phone VARCHAR(30) NOT NULL"); }
    }

    @Test void missingAccountUniquenessIsRejected() throws Exception {
        execute("ALTER TABLE bank_account DROP INDEX uk_bank_account_no");
        try { assertThrows(IllegalStateException.class, () -> BankSchemaValidator.validate(factory)); }
        finally { execute("ALTER TABLE bank_account ADD UNIQUE KEY uk_bank_account_no(account_no)"); }
    }

    @Test void logsContainSqlBoundArgumentsAndTotal() {
        var a = bank.openAccount("日志客户", "1");
        StringWriter output = new StringWriter();
        WriterAppender appender = new WriterAppender(new PatternLayout("%m%n"), output);
        Logger root = Logger.getRootLogger(); root.addAppender(appender);
        try { bank.account(a.getAccountNo()); }
        finally { root.removeAppender(appender); appender.close(); }
        String log = output.toString();
        assertTrue(log.contains("Preparing:")); assertTrue(log.contains("account_no = ?"));
        assertTrue(log.contains("Parameters: " + a.getAccountNo() + "(String)"));
        assertTrue(log.contains("Total: 1"));
    }
}
