package com.grocery.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void readsValuesFromTheEnvironment() {
        AppConfig c = AppConfig.from(Map.of("DB_URL", "jdbc:mysql://db/x", "DB_USER", "u",
                "DB_PASSWORD", "s3cret", "DB_POOL_SIZE", "4"));
        assertEquals("jdbc:mysql://db/x", c.dbUrl());
        assertEquals("u", c.dbUser());
        assertEquals(4, c.dbPoolSize());
    }

    @Test
    void hasLocalDefaults() {
        AppConfig c = AppConfig.from(Map.of());
        assertTrue(c.dbUrl().startsWith("jdbc:mysql://localhost:3306/grocery_inventory"));
        assertEquals(10, c.dbPoolSize());
    }

    @Test
    void neverPrintsThePassword() {
        assertFalse(AppConfig.from(Map.of("DB_PASSWORD", "s3cret")).toString().contains("s3cret"));
    }
}
