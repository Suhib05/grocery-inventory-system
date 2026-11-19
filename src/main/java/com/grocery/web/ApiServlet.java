package com.grocery.web;

import com.google.gson.JsonParseException;
import com.grocery.config.AppServices;
import com.grocery.model.User;
import com.grocery.service.ConflictException;
import com.grocery.service.NotFoundException;
import com.grocery.service.TooManyAttemptsException;
import com.grocery.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for JSON endpoints. Turns exceptions into consistent JSON error
 * responses with the right HTTP status, so each servlet only handles the happy path.
 */
public abstract class ApiServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ApiServlet.class.getName());

    public static final String SESSION_USER = "user";
    public static final String SESSION_CSRF = "csrfToken";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            super.service(req, resp);
        } catch (ValidationException e) {
            error(resp, 400, e.getMessage(), e.fieldErrors());
        } catch (JsonParseException e) {
            error(resp, 400, "The request body isn't valid JSON for this request.", Map.of());
        } catch (ForbiddenException e) {
            error(resp, 403, e.getMessage(), Map.of());
        } catch (NotFoundException e) {
            error(resp, 404, e.getMessage(), Map.of());
        } catch (ConflictException e) {
            error(resp, 409, e.getMessage(), Map.of());
        } catch (TooManyAttemptsException e) {
            error(resp, 429, e.getMessage(), Map.of());
            resp.setHeader("Retry-After", String.valueOf(Math.max(1, e.retryAfter().toSeconds())));
        } catch (RuntimeException e) {
            // Log the details for us; don't leak them to the client.
            LOG.log(Level.SEVERE, "Unhandled error on " + req.getMethod() + " " + req.getRequestURI(), e);
            error(resp, 500, "Something went wrong on our side. Please try again.", Map.of());
        }
    }

    protected AppServices services() {
        return (AppServices) getServletContext().getAttribute(AppServices.ATTRIBUTE);
    }

    /** The logged-in user. The auth filter guarantees there is one for API calls. */
    protected static User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SESSION_USER);
        if (user == null) throw new ForbiddenException("Please log in.");
        return user;
    }

    protected static User requireAdmin(HttpServletRequest req) {
        User user = currentUser(req);
        if (!user.isAdmin()) throw new ForbiddenException("Only an administrator can do this.");
        return user;
    }

    protected static <T> T readJson(HttpServletRequest req, Class<T> type) throws IOException {
        try (Reader reader = req.getReader()) {
            T value = JsonUtil.GSON.fromJson(reader, type);
            if (value == null) throw new ValidationException("A JSON request body is required.");
            return value;
        }
    }

    protected static void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        JsonUtil.GSON.toJson(body, resp.getWriter());
    }

    protected static void error(HttpServletResponse resp, int status, String message,
                                Map<String, String> fieldErrors) throws IOException {
        if (resp.isCommitted()) return;
        resp.reset();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        if (!fieldErrors.isEmpty()) body.put("fieldErrors", fieldErrors);
        writeJson(resp, status, body);
    }

    /** Parses the numeric id in a path like "/42" or "/42/void"; throws 404 if missing or not a number. */
    protected static int pathId(HttpServletRequest req) {
        String[] parts = pathParts(req);
        if (parts.length == 0) throw new NotFoundException("Not found.");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new NotFoundException("Not found.");
        }
    }

    protected static String[] pathParts(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return new String[0];
        return info.substring(1).split("/");
    }

    protected static Integer intParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("'" + name + "' must be a whole number.");
        }
    }

    /** Thrown when the user is logged in but not allowed to do something. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }
}
