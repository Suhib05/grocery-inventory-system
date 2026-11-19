package com.grocery.web.api;

import com.grocery.model.User;
import com.grocery.web.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * POST /api/auth/login   {username, password}
 * POST /api/auth/logout
 * GET  /api/auth/me      the current user and their CSRF token
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends ApiServlet {

    private static final SecureRandom RANDOM = new SecureRandom();

    private record LoginRequest(String username, String password) { }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (action) {
            case "/login" -> login(req, resp);
            case "/logout" -> logout(req, resp);
            default -> error(resp, 404, "Not found.", Map.of());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"/me".equals(req.getPathInfo())) {
            error(resp, 404, "Not found.", Map.of());
            return;
        }
        HttpSession session = req.getSession(false);
        writeJson(resp, 200, sessionInfo(currentUser(req), (String) session.getAttribute(SESSION_CSRF)));
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest body = readJson(req, LoginRequest.class);
        Optional<User> user = services().auth().login(body.username(), body.password());
        if (user.isEmpty()) {
            error(resp, 401, "Invalid username or password.", Map.of());
            return;
        }
        // New session id after login prevents session fixation.
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        String csrf = newToken();
        session.setAttribute(SESSION_USER, user.get());
        session.setAttribute(SESSION_CSRF, csrf);
        writeJson(resp, 200, sessionInfo(user.get(), csrf));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        writeJson(resp, 200, Map.of("ok", true));
    }

    private static Map<String, Object> sessionInfo(User user, String csrf) {
        return Map.of("user", user, "csrfToken", csrf);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
