package com.grocery.web;

import com.google.gson.JsonParseException;
import com.grocery.service.ProductInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonUtilTest {

    @Test
    void readsRequestBodiesIntoRecords() {
        ProductInput in = JsonUtil.GSON.fromJson(
                "{\"name\":\"Milk\",\"categoryId\":1,\"quantity\":4,\"price\":\"5.49\",\"expiryDate\":\"2026-03-20\"}",
                ProductInput.class);
        assertEquals("Milk", in.name());
        assertEquals(new BigDecimal("5.49"), in.price());
        assertEquals(LocalDate.of(2026, 3, 20), in.expiryDate());
        assertNull(in.minStock());
    }

    @Test
    void blankOrNullDatesBecomeNull() {
        assertNull(JsonUtil.GSON.fromJson("{\"expiryDate\":\"\"}", ProductInput.class).expiryDate());
        assertNull(JsonUtil.GSON.fromJson("{\"expiryDate\":null}", ProductInput.class).expiryDate());
    }

    @Test
    void badDatesAreRejected() {
        assertThrows(JsonParseException.class,
                () -> JsonUtil.GSON.fromJson("{\"expiryDate\":\"31/12/2026\"}", ProductInput.class));
    }

    @Test
    void writesDatesAsIsoStrings() {
        assertEquals("\"2026-03-20\"", JsonUtil.GSON.toJson(LocalDate.of(2026, 3, 20)));
    }
}
