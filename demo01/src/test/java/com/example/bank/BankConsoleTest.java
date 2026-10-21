package com.example.bank;

import org.example.BankConsole;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class BankConsoleTest extends BankTestSupport {
    private String console(String input) {
        var bytes = new ByteArrayOutputStream();
        try (var out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new BankConsole(bank, new StringReader(input), out).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void exitsAndHandlesEndOfInput() {
        assertTrue(console("0\n").contains("已退出银行系统"));
        assertTrue(console("").contains("输入结束"));
        assertTrue(console("4\n\n姓名\n").contains("输入结束"));
        assertTrue(bank.findCustomers(null).isEmpty());
    }

    @Test void invalidMenuAndNumericInputReturnToMenu() {
        String out = console("无效\n8\nBA1\nabc\n0\n");
        assertTrue(out.contains("无效菜单选项")); assertTrue(out.contains("必须是有效数字")); assertTrue(out.contains("已退出"));
    }

    @Test void createCustomerUpdateAndList() {
        String out = console("2\n客户甲\n13800000000\n1\n客户\n138\n0\n");
        assertTrue(out.contains("新增成功")); assertTrue(out.contains("客户甲"));
        var c = bank.findCustomers(null).get(0);
        out = console("3\n"+c.getId()+"\n客户乙\n\n1\n客户乙\n\n0\n");
        assertTrue(out.contains("更新客户数：1")); assertTrue(out.contains("name=客户乙"));
        assertEquals("13800000000", bank.findCustomers(null).get(0).getPhone());
    }

    @Test void depositsWithdrawalsAndPageThroughRealMenu() {
        var a = bank.openAccount("客户", "1"); String no = a.getAccountNo();
        String out = console("6\n"+no+"\n100.50\n7\n"+no+"\n20.25\n5\n"+no+"\n8\n"+no+"\n1\n10\n9\n"+a.getCustomerId()+"\n0\n");
        assertTrue(out.contains("存款成功")); assertTrue(out.contains("取款成功"));
        assertTrue(out.contains("balance=80.25")); assertTrue(out.contains("流水总数：2"));
    }

    @Test void invalidMoneyAndInsufficientBalanceAreRecoverable() {
        var a = bank.openAccount("客户", "1"); String no = a.getAccountNo();
        String out = console("6\n"+no+"\nabc\n6\n"+no+"\n0\n7\n"+no+"\n1\n0\n");
        assertTrue(out.contains("必须是有效数字")); assertTrue(out.contains("金额必须大于零"));
        assertTrue(out.contains("余额不足")); assertEquals(0, bank.transactions(no,1,10).getTotal());
    }

    @Test void accountOpeningMenuSupportsBothCustomerChoices() {
        assertTrue(console("4\n\n开户客户\n1\n0\n").contains("开户成功，账号：BA"));
        var customer = bank.findCustomers(null).get(0);
        assertTrue(console("4\n"+customer.getId()+"\n0\n").contains("开户成功"));
        assertEquals(2, bank.accounts(customer.getId()).size());
    }
}
