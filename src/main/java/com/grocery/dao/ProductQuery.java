package com.grocery.dao;

/**
 * Search, filter and sort options for listing products.
 * Sort and order are checked against a whitelist before reaching SQL.
 */
public record ProductQuery(String keyword, Integer categoryId, SortField sort, boolean descending) {

    public enum SortField {
        NAME("p.name"), QUANTITY("p.quantity"), PRICE("p.price"), EXPIRY_DATE("p.expiry_date");

        private final String column;
        SortField(String column) { this.column = column; }
        public String column() { return column; }

        /** Parses a request value such as "expiry_date"; unknown values fall back to NAME. */
        public static SortField parse(String value) {
            if (value == null) return NAME;
            for (SortField f : values()) {
                if (f.name().equalsIgnoreCase(value)) return f;
            }
            return NAME;
        }
    }

    public static ProductQuery all() { return new ProductQuery(null, null, SortField.NAME, false); }
}
