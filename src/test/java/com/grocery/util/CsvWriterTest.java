package com.grocery.util;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvWriterTest {

    @Test
    void plainValuesAreWrittenAsIs() {
        assertEquals("Milk", CsvWriter.escape("Milk"));
        assertEquals("5.49", CsvWriter.escape(new BigDecimal("5.49")));
        assertEquals("", CsvWriter.escape(null));
    }

    @Test
    void commasQuotesAndNewlinesAreQuoted() {
        assertEquals("\"Eggs, Large\"", CsvWriter.escape("Eggs, Large"));
        assertEquals("\"12\"\" Pizza\"", CsvWriter.escape("12\" Pizza"));
        assertEquals("\"a\nb\"", CsvWriter.escape("a\nb"));
    }

    @Test
    void formulaLikeTextIsNeutralised() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvWriter.escape("=HYPERLINK(\"x\")"));
        assertEquals("'+1", CsvWriter.escape("+1"));
        assertEquals("'@SUM(A1)", CsvWriter.escape("@SUM(A1)"));
        assertEquals("'-cmd", CsvWriter.escape("-cmd"));
    }

    @Test
    void negativeNumbersAreLeftAlone() {
        assertEquals("-5", CsvWriter.escape("-5"));
        assertEquals("-2.50", CsvWriter.escape("-2.50"));
        assertEquals("-3", CsvWriter.escape(-3));
    }

    @Test
    void rowsUseCrLf() throws Exception {
        StringWriter out = new StringWriter();
        new CsvWriter(out).writeRow(Arrays.asList(1, "Milk", null, new BigDecimal("5.49")));
        assertEquals("1,Milk,,5.49\r\n", out.toString());
    }
}
