package com.financetracker.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Small CSV parser: quoted fields may contain commas, escaped quotes ("")
 * and line breaks. No third-party dependency needed for bank-sized files.
 */
public final class CsvParser {

    private CsvParser() {
    }

    /** Parse CSV text into rows of fields. Returns an empty list for empty text. */
    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return rows;
        }
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean atFieldStart = true;
        boolean rowEnded = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
                rowEnded = false;
            } else if (c == '"' && atFieldStart) {
                inQuotes = true;
                atFieldStart = false;
                rowEnded = false;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                atFieldStart = true;
                rowEnded = false;
            } else if (c == '\r') {
                // Lone \r ends a row; \r\n is consumed as one line break.
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                rows.add(fields.toArray(new String[0]));
                fields = new ArrayList<>();
                atFieldStart = true;
                rowEnded = true;
            } else if (c == '\n') {
                fields.add(field.toString());
                field.setLength(0);
                rows.add(fields.toArray(new String[0]));
                fields = new ArrayList<>();
                atFieldStart = true;
                rowEnded = true;
            } else {
                field.append(c);
                atFieldStart = false;
                rowEnded = false;
            }
        }
        if (!rowEnded) {
            fields.add(field.toString());
            rows.add(fields.toArray(new String[0]));
        }
        return rows;
    }
}
