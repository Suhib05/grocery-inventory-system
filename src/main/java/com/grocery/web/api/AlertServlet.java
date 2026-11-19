package com.grocery.web.api;

import com.grocery.web.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/alerts?days=7 : expired products and products expiring within N days. */
@WebServlet("/api/alerts")
public class AlertServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer days = intParam(req, "days");
        writeJson(resp, 200, services().products().expiryAlerts(days == null ? 7 : days));
    }
}
