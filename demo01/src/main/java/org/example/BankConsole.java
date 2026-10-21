package org.example;

import com.example.entity.Customer;
import com.example.service.BankService;
import com.example.util.ConsoleStyle;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.log4j.Logger;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/** 可注入输入输出的菜单；只调用 Service，不直接操作 Mapper。 */
public final class BankConsole {
    private static final Logger LOG = Logger.getLogger(BankConsole.class);
    private final BankService bank;
    private final BufferedReader input;
    private final PrintStream out;

    public BankConsole(BankService bank, Reader input, PrintStream out) {
        this.bank = Objects.requireNonNull(bank);
        this.input = new BufferedReader(Objects.requireNonNull(input));
        this.out = Objects.requireNonNull(out);
    }

    public void run() {
        banner();
        while (true) {
            menu();
            try {
                switch (read("请选择：")) {
                    case "0" -> { info("已退出银行系统"); return; }
                    case "1" -> printResults("条件查询结果", bank.findCustomers(new Customer(null, optional("姓名关键词："), optional("电话关键词："))));
                    case "2" -> success("新增成功：" + bank.addCustomer(read("姓名："), read("电话：")));
                    case "3" -> {
                        Customer c = new Customer(integer("客户编号："), optional("新姓名（留空不修改）："), optional("新电话（留空不修改）："));
                        success("更新客户数：" + bank.updateCustomer(c));
                    }
                    case "4" -> openAccount();
                    case "5" -> balanceQuery();
                    case "6" -> deposit();
                    case "7" -> withdraw();
                    case "8" -> transactionPage();
                    case "9" -> customerStatement();
                    default -> warn("无效菜单选项，请重新选择");
                }
            } catch (EndOfInput e) {
                info("输入结束，已退出银行系统"); return;
            } catch (NumberFormatException e) {
                failure("输入错误：编号、页码或金额必须是有效数字");
            } catch (IllegalArgumentException e) {
                failure("操作失败：" + e.getMessage());
            } catch (PersistenceException e) {
                failure("数据库操作失败：请检查连接或数据约束");
                LOG.error("银行操作失败，事务已回滚", e);
            }
        }
    }

    /** 4 开户：输入已有客户编号，或留空登记新客户。 */
    private void openAccount() {
        String id = read("已有客户编号（留空登记新客户）：");
        var account = id.isEmpty() ? bank.openAccount(read("姓名："), read("电话："))
                : bank.openAccount(Integer.valueOf(id));
        success("开户成功，账号：" + account.getAccountNo() + "，余额：" + account.getBalance().toPlainString());
    }

    /** 5 余额查询。 */
    private void balanceQuery() { info("账户信息：" + bank.account(read("账号："))); }

    /** 6 存款。 */
    private void deposit() { success("存款成功：" + bank.deposit(read("账号："), amount())); }

    /** 7 取款。 */
    private void withdraw() { success("取款成功：" + bank.withdraw(read("账号："), amount())); }

    /** 8 流水分页。 */
    private void transactionPage() {
        var page = bank.transactions(read("账号："), number("页码："), number("每页条数："));
        info("流水总数：" + page.getTotal());
        printResults("流水明细", page.getRecords());
    }

    /** 9 客户账单：列出客户名下的全部账户。 */
    private void customerStatement() { printResults("客户账单", bank.accounts(integer("客户编号："))); }

    /** 总显示宽度（含边框），可用 -Dconsole.width=120 调整。 */
    private static final int BOX_WIDTH = Math.max(60, Integer.getInteger("console.width", 80));

    private void banner() {
        out.println(ConsoleStyle.cyan("╭" + "─".repeat(BOX_WIDTH - 2) + "╮"));
        bannerLine("银行存取款系统", ConsoleStyle.bold("银行存取款系统"));
        bannerLine("查询结果：青色框内逐条列出；执行过程：灰色 [日志] 行；✔ 成功  ✘ 失败  ! 提示",
                "查询结果：" + ConsoleStyle.cyan("青色框内逐条列出")
                        + "；执行过程：" + ConsoleStyle.dim("灰色 [日志] 行")
                        + "；" + ConsoleStyle.green("✔ 成功") + "  " + ConsoleStyle.red("✘ 失败") + "  " + ConsoleStyle.yellow("! 提示"));
        out.println(ConsoleStyle.cyan("╰" + "─".repeat(BOX_WIDTH - 2) + "╯"));
        out.println();
    }

    private void bannerLine(String plain, String styled) {
        int pad = BOX_WIDTH - 4 - displayWidth(plain);
        out.println(ConsoleStyle.cyan("│ ") + styled + " ".repeat(Math.max(0, pad)) + ConsoleStyle.cyan(" │"));
    }

    /** 菜单用灰色弱化，避免和查询结果抢注意力。 */
    private void menu() {
        out.println(ConsoleStyle.dim("1查询客户 2新增客户 3修改客户 4开户 5余额查询"));
        out.println(ConsoleStyle.dim("6存款 7取款 8流水分页 9客户账单 0退出系统"));
    }

    /** 查询结果统一放进青色边框，并在标题里标注条数，与运行过程明确区分。 */
    private void printResults(String title, List<?> rows) {
        String header = title + " · 共 " + rows.size() + " 条";
        int pad = BOX_WIDTH - 5 - displayWidth(header);
        out.println(ConsoleStyle.cyan("┌─ ") + ConsoleStyle.bold(header) + " "
                + ConsoleStyle.cyan("─".repeat(Math.max(0, pad)) + "┐"));
        if (rows.isEmpty()) {
            contentLine("暂无记录");
        } else {
            int index = 1;
            for (Object row : rows) contentLine(index++ + ". " + row);
        }
        out.println(ConsoleStyle.cyan("└" + "─".repeat(BOX_WIDTH - 2) + "┘"));
    }

    private void contentLine(String text) {
        List<String> parts = wrap(text, BOX_WIDTH - 7);
        for (int i = 0; i < parts.size(); i++) {
            // 续行缩进三格，表示同一条记录；宽度按实际缩进重新计算，保持右边框对齐。
            String indent = i == 0 ? "│ " : "│   ";
            int pad = BOX_WIDTH - displayWidth(indent) - displayWidth(parts.get(i)) - 2;
            out.println(ConsoleStyle.cyan(indent) + parts.get(i) + " ".repeat(Math.max(0, pad)) + ConsoleStyle.cyan(" │"));
        }
    }

    private void success(String text) { out.println(ConsoleStyle.green("✔ " + text)); }
    private void failure(String text) { out.println(ConsoleStyle.red("✘ " + text)); }
    private void warn(String text) { out.println(ConsoleStyle.yellow("! " + text)); }
    private void info(String text) { out.println(ConsoleStyle.bold("· " + text)); }

    /** 按显示宽度换行（优先在空格处断行），保证中英文混排时边框不被撑破。 */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int i = start, used = 0, lastBreak = -1;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                int cpWidth = codePointWidth(cp);
                if (used + cpWidth > width) break;
                used += cpWidth;
                i += Character.charCount(cp);
                if (cp == ' ') lastBreak = i;
            }
            if (i >= text.length()) { lines.add(text.substring(start)); break; }
            int end = lastBreak > start ? lastBreak : Math.max(i, start + Character.charCount(text.codePointAt(start)));
            lines.add(text.substring(start, end).stripTrailing());
            start = end;
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static int displayWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            width += codePointWidth(cp);
            i += Character.charCount(cp);
        }
        return width;
    }

    /** 东亚宽字符占两列，其余按一列处理（忽略组合符号等零宽字符的极端情况）。 */
    private static int codePointWidth(int cp) {
        return (cp >= 0x1100 && (cp <= 0x115F || cp == 0x2329 || cp == 0x232A
                || (cp >= 0x2E80 && cp <= 0xA4CF && cp != 0x303F)
                || (cp >= 0xAC00 && cp <= 0xD7A3)
                || (cp >= 0xF900 && cp <= 0xFAFF)
                || (cp >= 0xFE30 && cp <= 0xFE6F)
                || (cp >= 0xFF00 && cp <= 0xFF60)
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x1F300 && cp <= 0x1F64F)
                || (cp >= 0x1F900 && cp <= 0x1F9FF)
                || (cp >= 0x20000 && cp <= 0x2FFFD)
                || (cp >= 0x30000 && cp <= 0x3FFFD))) ? 2 : 1;
    }
    private int integer(String prompt) { return Integer.parseInt(read(prompt)); }
    private long number(String prompt) { return Long.parseLong(read(prompt)); }
    private BigDecimal amount() { return new BigDecimal(read("金额：")); }
    private String optional(String prompt) { String value = read(prompt); return value.isEmpty() ? null : value; }
    private String read(String prompt) {
        out.print(ConsoleStyle.cyan(prompt)); out.flush();
        try {
            String value = input.readLine();
            if (value == null) throw new EndOfInput();
            return value.strip();
        } catch (IOException e) { throw new UncheckedIOException("读取输入失败", e); }
    }
    private static final class EndOfInput extends RuntimeException {}
}
