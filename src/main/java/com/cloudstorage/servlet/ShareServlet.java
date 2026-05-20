package com.cloudstorage.servlet;

import com.cloudstorage.model.User;
import com.cloudstorage.service.ShareService;
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
import java.time.Instant;
import java.util.Map;

/**
 * Сервлет управления общим доступом
 * POST /api/shares/public - создание публичной ссылки
 * POST /api/shares/user - предоставление доступа пользователю
 */
@WebServlet("/api/shares/*")
public class ShareServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ShareServlet.class);
    private final ShareService shareService = new ShareService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);
        if (currentUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            return;
        }

        try {
            String body = req.getReader().lines().reduce("", String::concat);
            Map<String, Object> data = JsonUtils.fromJson(body, Map.class);

            switch (path) {
                case "/public" -> handleCreatePublicShare(req, resp, currentUser, data);
                case "/user" -> handleCreateUserShare(req, resp, currentUser, data);
                default -> sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private void handleCreatePublicShare(HttpServletRequest req, HttpServletResponse resp, 
                                          User currentUser, Map<String, Object> data) throws IOException {
        int itemId = ((Number) data.get("itemId")).intValue();
        String itemType = (String) data.get("itemType");
        String expiresAtStr = (String) data.get("expiresAt");
        
        Instant expiresAt = null;
        if (expiresAtStr != null && !expiresAtStr.isEmpty()) {
            expiresAt = Instant.parse(expiresAtStr);
        }

        var share = shareService.createPublicShare(currentUser.getId(), itemId, itemType, expiresAt);
        
        String shareUrl = req.getContextPath() + "/shared/" + share.getToken();
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(JsonUtils.toJson(Map.of("shareUrl", shareUrl, "token", share.getToken())));
    }

    private void handleCreateUserShare(HttpServletRequest req, HttpServletResponse resp, 
                                        User currentUser, Map<String, Object> data) throws IOException {
        int itemId = ((Number) data.get("itemId")).intValue();
        String itemType = (String) data.get("itemType");
        int targetUserId = ((Number) data.get("targetUserId")).intValue();
        String permission = (String) data.get("permission");

        var share = shareService.createUserShare(currentUser.getId(), itemId, itemType, targetUserId, permission);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(JsonUtils.toJson(share));
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

        try {
            String path = req.getPathInfo();
            if ("/list".equals(path)) {
                var shares = shareService.getUserShares(currentUser.getId());
                resp.getWriter().write(JsonUtils.toJson(shares));
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
            logger.error("Ошибка при работе с шарингами: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtils.errorJson("Внутренняя ошибка сервера"));
        }
    }
}
