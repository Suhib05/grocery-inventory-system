package com.grocery.dao;

import com.grocery.dao.jdbc.JdbcProductDao;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcProductDaoTest {

    private static String escapeLike(String s) throws Exception {
        Method m = JdbcProductDao.class.getDeclaredMethod("escapeLike", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, s);
    }

    @Test
    void likeWildcardsInSearchTextAreEscaped() throws Exception {
        assertEquals("50!% off", escapeLike("50% off"));
        assertEquals("a!_b", escapeLike("a_b"));
        assertEquals("wow!!", escapeLike("wow!"));
        assertEquals("milk", escapeLike("milk"));
    }

    @Test
    void sortFieldParsingFallsBackToName() {
        assertEquals(ProductQuery.SortField.EXPIRY_DATE, ProductQuery.SortField.parse("expiry_date"));
        assertEquals(ProductQuery.SortField.NAME, ProductQuery.SortField.parse("name; DROP TABLE products"));
        assertEquals(ProductQuery.SortField.NAME, ProductQuery.SortField.parse(null));
    }
}
