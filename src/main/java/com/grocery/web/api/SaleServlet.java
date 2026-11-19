package com.grocery.web.api;

import com.grocery.model.Sale;
import com.grocery.model.User;
import com.grocery.service.SaleInput;
import com.grocery.service.ValidationException;
import com.grocery.web.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * GET  /api/sales?from=YYYY-MM-DD&to=YYYY-MM-DD   sales report (default: last 30 days)
 * POST /api/sales                                 record a sale {productId, quantity, saleDate?}
 * POST /api/sales/{id}/void                       void a sale and restore stock (admin only)
 */
@WebServlet("/api/sales/*")
public class SaleServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LocalDate to = dateParam(req, "to", LocalDate.now());
        LocalDate from = dateParam(req, "from", to.minusDays(29));
        writeJson(resp, 200, services().sales().report(from, to));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] parts = pathParts(req);
        if (parts.length == 0) {
            Sale sale = services().sales().record(readJson(req, SaleInput.class), currentUser(req));
            writeJson(resp, 201, sale);
        } else if (parts.length == 2 && parts[1].equals("void")) {
            User admin = requireAdmin(req); // check permission before looking anything up
            services().sales().voidSale(pathId(req), admin);
            writeJson(resp, 200, Map.of("ok", true));
        } else {
            error(resp, 404, "Not found.", Map.of());
        }
    }

    private static LocalDate dateParam(HttpServletRequest req, String name, LocalDate fallback) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return fallback;
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("'" + name + "' must be a date like 2026-01-31.");
        }
    }
}
