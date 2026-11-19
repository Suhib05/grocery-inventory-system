package com.grocery.web.filter;

import com.grocery.web.ApiServlet;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Protects every page and API call:
 * <ul>
 *   <li>Not logged in: pages redirect to the login page, API calls get 401.</li>
 *   <li>API calls that change data must send the session's CSRF token in the
 *       {@code X-CSRF-Token} header, which a third-party site can't read or forge.</li>
 * </ul>
 */
public class AuthFilter implements Filter {

    public static final String CSRF_HEADER = "X-CSRF-Token";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/login.html", "/js/api.js", "/js/login.js", "/favicon.svg", "/api/auth/login");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(ApiServlet.SESSION_USER) != null;
        boolean api = path.startsWith("/api/");

        if (path.equals("/login.html") && loggedIn) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (!loggedIn) {
            if (api) sendJsonError(resp, 401, "Your session has expired. Please log in again.");
            else     resp.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }
        if (api && !SAFE_METHODS.contains(req.getMethod()) && !validCsrf(req, session)) {
            sendJsonError(resp, 403, "Security token missing or invalid. Please reload the page.");
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path) || path.startsWith("/css/");
    }

    private static boolean validCsrf(HttpServletRequest req, HttpSession session) {
        String expected = (String) session.getAttribute(ApiServlet.SESSION_CSRF);
        String actual = req.getHeader(CSRF_HEADER);
        return expected != null && actual != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                         actual.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"error\":\"" + message + "\"}"); // messages are constants, no escaping needed
    }
}
