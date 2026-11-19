package com.grocery.web;

import com.grocery.web.filter.AuthFilter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterTest {

    private static boolean isPublic(String path) throws Exception {
        Method m = AuthFilter.class.getDeclaredMethod("isPublic", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, path);
    }

    @Test
    void loginPageAndStylesArePublic() throws Exception {
        assertTrue(isPublic("/login.html"));
        assertTrue(isPublic("/api/auth/login"));
        assertTrue(isPublic("/css/main.css"));
    }

    @Test
    void appPagesAndDataNeedLogin() throws Exception {
        assertFalse(isPublic("/"));
        assertFalse(isPublic("/dashboard.html"));
        assertFalse(isPublic("/api/products"));
        assertFalse(isPublic("/api/auth/me"));
        assertFalse(isPublic("/js/dashboard.js"));
    }
}
