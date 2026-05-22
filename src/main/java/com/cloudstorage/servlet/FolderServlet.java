package com.cloudstorage.servlet;

import com.cloudstorage.model.Folder;
import com.cloudstorage.model.User;
import com.cloudstorage.service.FolderService;
import com.cloudstorage.util.JsonUtils;
import com.cloudstorage.util.SessionUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Сервлет управления папками
 * POST /api/folders - создание папки
 * GET /api/folders/tree - дерево папок
 * DELETE /api/folders?id={folderId} - удаление
 */
@WebServlet("/api/folders/*")
public class FolderServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(FolderServlet.class);
    private final FolderService folderService = new FolderService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);
        if (currentUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            Map<String, Object> data = JsonUtils.fromJson(body, Map.class);

            String name = (String) data.get("name");
            Object parentIdObj = data.get("parentId");
            Integer parentId = null;
            if (parentIdObj != null) {
                parentId = ((Number) parentIdObj).intValue();
            }

            Folder folder = folderService.createFolder(currentUser.getId(), name, parentId);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtils.toJson(folder));
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

        try {
            String path = req.getPathInfo();
            if ("/tree".equals(path)) {
                var tree = folderService.getFolderTree(currentUser.getId());
                resp.getWriter().write(JsonUtils.toJson(tree));
            } else if (path == null || "/".equals(path)) {
                // Получить папки в заданной родительской папке (или корневые)
                String parentIdStr = req.getParameter("parentId");
                Integer parentId = null;
                if (parentIdStr != null && !parentIdStr.isEmpty() && !"null".equals(parentIdStr)) {
                    parentId = Integer.parseInt(parentIdStr);
                }
                var folders = folderService.getFoldersByParent(currentUser.getId(), parentId);
                resp.getWriter().write(JsonUtils.toJson(folders));
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);
        if (currentUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            return;
        }

        try {
            String folderIdStr = req.getParameter("id");
            if (folderIdStr == null || folderIdStr.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Необходим параметр id");
                return;
            }

            int folderId = Integer.parseInt(folderIdStr);
            folderService.deleteFolder(folderId, currentUser.getId());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtils.toJson(Map.of("message", "Папка удалена")));
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

}
