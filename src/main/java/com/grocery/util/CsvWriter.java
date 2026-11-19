package com.grocery.util;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Minimal RFC 4180 CSV writer.
 *
 * <p>Also protects against CSV/formula injection: a cell starting with
 * {@code = + - @}, a tab or a carriage return is prefixed with a single quote
 * so spreadsheet apps show it as text instead of running it as a formula.
 * Plain numbers (including negatives) are left alone.
 */
public final class CsvWriter {

    private final Writer out;

    public CsvWriter(Writer out) { this.out = out; }

    public void writeRow(List<?> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.write(',');
            out.write(escape(cells.get(i)));
        }
        out.write("\r\n");
    }

    static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (value instanceof String && isFormula(s)) s = "'" + s;
        boolean needsQuotes = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        return needsQuotes ? '"' + s.replace("\"", "\"\"") + '"' : s;
    }

    private static boolean isFormula(String s) {
        if (s.isEmpty()) return false;
        char c = s.charAt(0);
        if (c == '=' || c == '+' || c == '@' || c == '\t' || c == '\r') return true;
        return c == '-' && !s.matches("-\\d+(\\.\\d+)?");
    }
}
