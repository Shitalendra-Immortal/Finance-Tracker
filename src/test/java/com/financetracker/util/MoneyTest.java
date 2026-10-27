package com.financetracker.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void parsesPlainAmount() {
        assertEquals(new BigDecimal("250.50"), Money.parse("250.50"));
    }

    @Test
    void parseSetsTwoDecimalScale() {
        assertEquals(new BigDecimal("10.00"), Money.parse("10"));
    }

    @Test
    void parseRejectsWords() {
        assertThrows(IllegalArgumentException.class, () -> Money.parse("abc"));
    }

    @Test
    void parseRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Money.parse("   "));
    }

    @Test
    void formatAlwaysTwoDecimals() {
        assertEquals("₹10.00", Money.format(new BigDecimal("10")));
        assertEquals("₹0.00", Money.format(null));
    }

    @Test
    void formatUsesIndianGrouping() {
        assertEquals("₹1,000.00", Money.format(new BigDecimal("1000")));
        assertEquals("₹1,00,000.00", Money.format(new BigDecimal("100000")));
        assertEquals("−₹900.00", Money.format(new BigDecimal("-900")));
    }

    @Test
    void parseAcceptsFormattedInput() {
        assertEquals(new BigDecimal("1000.00"), Money.parse("₹1,000.00"));
        assertEquals(new BigDecimal("100000.00"), Money.parse("1,00,000"));
    }

    @Test
    void decimalMathIsExact() {
        // The classic float bug: 0.1 + 0.2 is not 0.3 with doubles. BigDecimal is exact.
        assertEquals(new BigDecimal("0.30"), new BigDecimal("0.10").add(new BigDecimal("0.20")));
    }
}
