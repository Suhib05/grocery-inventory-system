package com.grocery.dao;

import com.grocery.model.DailyRevenue;
import com.grocery.model.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleDao {

    /** Sales between two dates (inclusive), newest first, voided ones included. */
    List<Sale> findBetween(LocalDate from, LocalDate to);

    Optional<Sale> findById(int id);

    /**
     * Records a sale and lowers the product's stock in one transaction.
     * Stock is only lowered if enough is left, so two people selling the last
     * item at the same time can't push stock below zero.
     *
     * @return the new sale id
     * @throws InsufficientStockException if stock ran out (the transaction is rolled back)
     */
    int recordSale(int productId, int quantity, BigDecimal unitPrice, LocalDate saleDate, int userId);

    /**
     * Marks a sale as voided and puts its quantity back into stock, in one transaction.
     * Returns false if the sale doesn't exist or was already voided.
     */
    boolean voidSale(int saleId, int userId);

    /** Revenue from non-voided sales between two dates (inclusive). */
    BigDecimal revenueBetween(LocalDate from, LocalDate to);

    /** Revenue per day between two dates (inclusive). Days with no sales are omitted. */
    List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to);
}
