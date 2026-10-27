package com.financetracker.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvParserTest {

    @Test
    void parsesPlainRows() {
        List<String[]> rows = CsvParser.parse("2026-10-05,-250.50,Food\n2026-10-06,1000,Salary\n");
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"2026-10-05", "-250.50", "Food"}, rows.get(0));
        assertArrayEquals(new String[]{"2026-10-06", "1000", "Salary"}, rows.get(1));
    }

    @Test
    void quotedFieldsMayContainCommas() {
        List<String[]> rows = CsvParser.parse("2026-10-05,-250.50,\"Lunch, with friends\"\n");
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"2026-10-05", "-250.50", "Lunch, with friends"}, rows.get(0));
    }

    @Test
    void escapedQuotesBecomeOneQuote() {
        List<String[]> rows = CsvParser.parse("2026-10-05,-250.50,\"He said \"\"hi\"\"\"\n");
        assertArrayEquals(new String[]{"2026-10-05", "-250.50", "He said \"hi\""}, rows.get(0));
    }

    @Test
    void handlesWindowsLineEndings() {
        List<String[]> rows = CsvParser.parse("a,b\r\nc,d\r\n");
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{"c", "d"}, rows.get(1));
    }

    @Test
    void emptyTextGivesNoRows() {
        assertTrue(CsvParser.parse("").isEmpty());
        assertTrue(CsvParser.parse(null).isEmpty());
    }
}
