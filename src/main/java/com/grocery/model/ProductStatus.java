package com.grocery.model;

/** Derived stock/expiry state of a product, listed in priority order. */
public enum ProductStatus {
    OUT_OF_STOCK("Out of Stock"),
    EXPIRED("Expired"),
    EXPIRING_SOON("Expiring Soon"),
    LOW_STOCK("Low Stock"),
    IN_STOCK("In Stock");

    private final String label;

    ProductStatus(String label) { this.label = label; }

    public String label() { return label; }

    /** True for states that need someone's attention. */
    public boolean needsAttention() { return this != IN_STOCK; }
}
