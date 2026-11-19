package com.grocery.config;

import com.grocery.service.AuthService;
import com.grocery.service.DashboardService;
import com.grocery.service.ProductService;
import com.grocery.service.SaleService;

/** The app's service objects, built once at startup and shared by all servlets. */
public record AppServices(
        AuthService auth,
        ProductService products,
        SaleService sales,
        DashboardService dashboard) {

    public static final String ATTRIBUTE = AppServices.class.getName();
}
