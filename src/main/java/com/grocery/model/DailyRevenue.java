package com.grocery.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenue(LocalDate date, BigDecimal total) { }
