package com.grocery.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Sale(
        int id,
        int productId,
        String productName,
        int quantitySold,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        LocalDate saleDate,
        String recordedByName,
        LocalDateTime voidedAt,
        LocalDateTime createdAt) {

    public boolean isVoided() { return voidedAt != null; }
}
