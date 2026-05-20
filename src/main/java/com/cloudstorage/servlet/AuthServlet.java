package com.cloudstorage.servlet;

import com.cloudstorage.model.User;
import com.cloudstorage.service.AuthService;
import com.cloudstorage.util.JsonUtils;
import com.cloudstorage.util.SessionUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Сервлет аутентификации и регистрации
 * POST /api/auth/register - регистрация
 * POST /api/auth/login - вход
 * POST /api/auth/logout - выход
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String body = req.getReader().lines().reduce("", String::concat);
            Map<String, Object> data = JsonUtils.fromJson(body, Map.class);

            switch (path) {
                case "/register" -> handleRegister(req, resp, data);
                case "/login" -> handleLogin(req, resp, data);
                case "/logout" -> handleLogout(req, resp);
                default -> sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> data) throws IOException {
        String login = (String) data.get("login");
        String email = (String) data.get("email");
        String password = (String) data.get("password");

        User user = authService.register(login, email, password);
        // Автоматический вход после регистрации
        SessionUtils.setCurrentUser(req, user);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(JsonUtils.toJson(user));
        logger.info("Пользователь {} зарегистрирован и вошел в систему", user.getLogin());
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> data) throws IOException {
        String login = (String) data.get("login");
        String password = (String) data.get("password");

        User user = authService.login(login, password);
        SessionUtils.setCurrentUser(req, user);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(JsonUtils.toJson(user));
        logger.info("Пользователь {} вошел в систему", user.getLogin());
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = SessionUtils.getCurrentUser(req);
        SessionUtils.invalidateSession(req);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(JsonUtils.toJson(Map.of("message", "Выход выполнен успешно")));
        if (user != null) {
            logger.info("Пользователь {} вышел из системы", user.getLogin());
        }
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(JsonUtils.errorJson(message));
    }

    private void handleError(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof com.cloudstorage.exception.AppException appEx) {
            resp.setStatus(appEx.getStatusCode());
            resp.getWriter().write(JsonUtils.errorJson(appEx.getMessage()));
        } else {
            logger.error("Необработанная ошибка: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtils.errorJson("Внутренняя ошибка сервера"));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "POST");
        resp.getWriter().write(JsonUtils.errorJson("Method not allowed"));
    }
}
