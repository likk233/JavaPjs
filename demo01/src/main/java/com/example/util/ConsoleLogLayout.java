package com.example.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * 日志行统一弱化为灰色，让 SQL 执行过程在终端里作为“背景信息”，
 * 与菜单、查询结果的彩色输出区分开；不改变 PatternLayout 的原有内容。
 */
public final class ConsoleLogLayout extends PatternLayout {
    @Override
    public String format(LoggingEvent event) {
        String text = super.format(event);
        if (!ConsoleStyle.enabled() || text.isEmpty()) return text;
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) end--;
        return ConsoleStyle.dim(text.substring(0, end)) + text.substring(end);
    }
}
