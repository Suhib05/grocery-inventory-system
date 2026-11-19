package com.grocery.config;

import java.util.Map;

/**
 * Settings read from environment variables (or JVM system properties of the
 * same name), so no passwords live in the source code.
 */
public record AppConfig(String dbUrl, String dbUser, String dbPassword, int dbPoolSize) {

    public static AppConfig fromEnvironment() {
        return from(System.getenv());
    }

    static AppConfig from(Map<String, String> env) {
        return new AppConfig(
                value(env, "DB_URL", "jdbc:mysql://localhost:3306/grocery_inventory"
                        + "?characterEncoding=UTF-8"),
                value(env, "DB_USER", "grocery_app"),
                value(env, "DB_PASSWORD", ""),
                Integer.parseInt(value(env, "DB_POOL_SIZE", "10")));
    }

    private static String value(Map<String, String> env, String key, String fallback) {
        String v = System.getProperty(key, env.get(key));
        return v == null || v.isBlank() ? fallback : v;
    }

    /** Never print the password. */
    @Override
    public String toString() {
        return "AppConfig[dbUrl=" + dbUrl + ", dbUser=" + dbUser + ", dbPoolSize=" + dbPoolSize + "]";
    }
}
