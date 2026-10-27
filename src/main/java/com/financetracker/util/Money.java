package com.financetracker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single place money is parsed and formatted.
 * All money math in the app uses BigDecimal — never float or double —
 * so amounts like 0.10 + 0.20 come out exact instead of 0.30000000000000004.
 * Formatting happens only at display time; calculations never see a formatted string.
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Money() {
    }

    /** Parse user input, tolerating ₹, commas (Indian or Western grouping) and spaces. */
    public static BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter an amount.");
        }
        String cleaned = text.trim().replace("₹", "").replace(",", "").replace(" ", "");
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number, e.g. 250.50");
        }
    }

    /**
     * Indian currency format: ₹1,000.00, ₹1,00,000.00, always two decimals.
     * Negatives read −₹900.00 (minus before the symbol). Null reads as zero.
     * Grouping is done by hand (last 3 digits, then pairs) because the JDK's
     * en-IN formatter uses Western grouping on this runtime.
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        boolean negative = scaled.signum() < 0;
        String plain = scaled.abs().toPlainString();
        int dot = plain.indexOf('.');
        String intPart = dot >= 0 ? plain.substring(0, dot) : plain;
        String fracPart = dot >= 0 ? plain.substring(dot) : ".00";
        return (negative ? "−" : "") + "₹" + groupIndian(intPart) + fracPart;
    }

    /** Indian digit grouping: 12345678 -> 1,23,45,678; 1000 -> 1,000. */
    static String groupIndian(String digits) {
        int len = digits.length();
        if (len <= 3) {
            return digits;
        }
        String head = digits.substring(0, len - 3);
        String tail = digits.substring(len - 3);
        StringBuilder headGrouped = new StringBuilder();
        int i = head.length();
        while (i > 2) {
            headGrouped.insert(0, "," + head.substring(i - 2, i));
            i -= 2;
        }
        headGrouped.insert(0, head.substring(0, i));
        return headGrouped + "," + tail;
    }
}
