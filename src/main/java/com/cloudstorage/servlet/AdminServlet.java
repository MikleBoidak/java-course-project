package com.cloudstorage.servlet;

import com.cloudstorage.dao.UserDao;
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
import java.util.List;
import java.util.Map;

/**
 * Сервлет административных функций
 * PUT /api/admin/users/{userId}/quota - изменение квоты пользователя
 * GET /api/admin/users - список пользователей
 */
@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminServlet.class);
    private final AuthService authService = new AuthService();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);
        if (currentUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            return;
        }
        if (!currentUser.isAdmin()) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Требуется роль администратора");
            return;
        }

        try {
            // /api/admin/users/{userId}/quota
            if (path != null && path.matches("/users/\\d+/quota")) {
                String[] parts = path.split("/");
                int userId = Integer.parseInt(parts[2]);
                
                String body = req.getReader().lines().reduce("", String::concat);
                Map<String, Object> data = JsonUtils.fromJson(body, Map.class);
                long quota = ((Number) data.get("quota")).longValue();

                authService.updateQuota(userId, quota, currentUser.getId());
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(JsonUtils.toJson(Map.of("message", "Квота обновлена")));
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);
        if (currentUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            return;
        }
        if (!currentUser.isAdmin()) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Требуется роль администратора");
            return;
        }

        try {
            String path = req.getPathInfo();
            if ("/users".equals(path)) {
                UserDao userDao = new UserDao();
                List<User> users = userDao.findAll();
                // Убираем пароли
                users.forEach(u -> u.setPasswordHash(null));
                resp.getWriter().write(JsonUtils.toJson(users));
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
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
            logger.error("Ошибка админ-сервлета: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtils.errorJson("Внутренняя ошибка сервера"));
        }
    }
}
