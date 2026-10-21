package com.example.util;

/**
 * 终端 ANSI 样式工具：给菜单、查询结果、成功/失败提示和 SQL 日志分配不同颜色。
 * 自动检测失败时可用 -Dconsole.color=always|never 或环境变量 CONSOLE_COLOR 强制开关。
 */
public final class ConsoleStyle {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[90m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static final boolean ENABLED = detect();

    private ConsoleStyle() {}

    /** 当前终端是否输出颜色。 */
    public static boolean enabled() { return ENABLED; }

    public static String bold(String text) { return wrap(BOLD, text); }
    public static String dim(String text) { return wrap(DIM, text); }
    public static String red(String text) { return wrap(RED, text); }
    public static String green(String text) { return wrap(GREEN, text); }
    public static String yellow(String text) { return wrap(YELLOW, text); }
    public static String cyan(String text) { return wrap(CYAN, text); }

    private static String wrap(String code, String text) {
        return ENABLED ? code + text + RESET : text;
    }

    private static boolean detect() {
        String mode = System.getProperty("console.color", System.getenv("CONSOLE_COLOR"));
        if (mode != null) {
            if (mode.equalsIgnoreCase("never") || mode.equalsIgnoreCase("false") || mode.equalsIgnoreCase("plain")) return false;
            if (mode.equalsIgnoreCase("always") || mode.equalsIgnoreCase("true")) return true;
        }
        if (System.getenv("NO_COLOR") != null) return false;
        String term = System.getenv("TERM");
        if ("dumb".equalsIgnoreCase(term)) return false;
        // 管道/重定向时 System.console() 为 null；此时用 TERM 判断是否处于真实终端。
        return System.console() != null || (term != null && !term.isBlank());
    }
}
