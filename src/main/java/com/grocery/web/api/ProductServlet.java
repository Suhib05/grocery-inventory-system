package com.grocery.web.api;

import com.grocery.dao.ProductQuery;
import com.grocery.model.ProductStatus;
import com.grocery.model.ProductView;
import com.grocery.service.ProductInput;
import com.grocery.service.ValidationException;
import com.grocery.util.CsvWriter;
import com.grocery.web.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * GET    /api/products?q=&categoryId=&status=&sort=&order=   list
 * GET    /api/products/export?...same filters...              CSV download
 * GET    /api/products/{id}
 * POST   /api/products                                        create
 * PUT    /api/products/{id}                                   update
 * DELETE /api/products/{id}                                   archive (admin only)
 */
@WebServlet("/api/products/*")
public class ProductServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] parts = pathParts(req);
        if (parts.length == 0) {
            writeJson(resp, 200, list(req));
        } else if (parts.length == 1 && parts[0].equals("export")) {
            exportCsv(list(req), resp);
        } else {
            writeJson(resp, 200, services().products().get(pathId(req)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (pathParts(req).length != 0) {
            resp.sendError(405);
            return;
        }
        ProductView created = services().products().create(readJson(req, ProductInput.class));
        resp.setHeader("Location", req.getContextPath() + "/api/products/" + created.id());
        writeJson(resp, 201, created);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = pathId(req);
        writeJson(resp, 200, services().products().update(id, readJson(req, ProductInput.class)));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        requireAdmin(req);
        services().products().archive(pathId(req));
        resp.setStatus(204);
    }

    private List<ProductView> list(HttpServletRequest req) {
        ProductQuery query = new ProductQuery(
                req.getParameter("q"),
                intParam(req, "categoryId"),
                ProductQuery.SortField.parse(req.getParameter("sort")),
                "desc".equalsIgnoreCase(req.getParameter("order")));
        return services().products().list(query, parseStatus(req.getParameter("status")));
    }

    private static ProductStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ProductStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown status. Use one of " + Arrays.toString(ProductStatus.values()));
        }
    }

    private static void exportCsv(List<ProductView> products, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/csv");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"products-" + LocalDate.now() + ".csv\"");
        CsvWriter csv = new CsvWriter(resp.getWriter());
        csv.writeRow(List.of("ID", "Name", "Category", "Quantity", "Price", "Min Stock", "Expiry Date", "Status"));
        for (ProductView p : products) {
            csv.writeRow(Arrays.asList(p.id(), p.name(), p.categoryName(), p.quantity(), p.price(),
                    p.minStock(), p.expiryDate(), p.statusLabel()));
        }
    }
}
