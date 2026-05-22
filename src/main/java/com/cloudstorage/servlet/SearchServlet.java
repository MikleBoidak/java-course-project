package com.cloudstorage.servlet;

import com.cloudstorage.model.User;
import com.cloudstorage.service.FileService;
import com.cloudstorage.util.JsonUtils;
import com.cloudstorage.util.SessionUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Сервлет поиска файлов
 * GET /api/search?query=...&type=...
 */
@WebServlet("/api/search")
public class SearchServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(SearchServlet.class);
    private final FileService fileService = new FileService();

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
            String query = req.getParameter("query");
            String type = req.getParameter("type");

            if (query == null || query.trim().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Необходим параметр query");
                return;
            }

            var results = fileService.searchFiles(currentUser.getId(), query.trim(), type);
            resp.getWriter().write(JsonUtils.toJson(results));
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "GET");
        resp.getWriter().write(JsonUtils.errorJson("Method not allowed"));
    }

}
