package com.example.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** 控制台、Service 与自定义 Mapper 共用的边界校验。 */
public final class BankValidation {
    public static final BigDecimal MAX_BALANCE = new BigDecimal("9999999999999999.99");
    private BankValidation() {}

    public static String text(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        String result = value.strip();
        if (result.codePointCount(0, result.length()) > maxLength)
            throw new IllegalArgumentException(label + "不能超过" + maxLength + "个字符");
        return result;
    }

    public static void id(Number id) {
        if (id == null || id.longValue() <= 0) throw new IllegalArgumentException("编号必须为正整数");
    }

    public static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException("金额必须大于零");
        try {
            BigDecimal normalized = value.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(MAX_BALANCE) > 0) throw new IllegalArgumentException("金额超出允许范围");
            return normalized;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("金额最多保留两位有效小数，不允许舍入", e);
        }
    }

    public static void page(long current, long size) {
        if (current < 1 || size < 1) throw new IllegalArgumentException("页码和每页条数必须大于零");
        try { Math.multiplyExact(current - 1, size); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("分页偏移量超出范围", e); }
    }

    public static void nonEmpty(Collection<?> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("批量参数不能为空");
        if (values.stream().anyMatch(java.util.Objects::isNull))
            throw new IllegalArgumentException("批量参数不能包含 null");
    }
}
