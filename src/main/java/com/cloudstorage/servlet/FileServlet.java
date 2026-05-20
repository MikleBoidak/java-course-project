package com.cloudstorage.servlet;

import com.cloudstorage.model.FileItem;
import com.cloudstorage.model.User;
import com.cloudstorage.service.FileService;
import com.cloudstorage.util.JsonUtils;
import com.cloudstorage.util.SessionUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Сервлет управления файлами
 * POST /api/files/upload - загрузка файла
 * GET /api/files/download?id={fileId} - скачивание
 * GET /api/files/thumbnail?id={fileId} - получение миниатюры
 * GET /api/files?folderId={folderId} - список файлов
 * DELETE /api/files?id={fileId} - удаление
 */
@WebServlet("/api/files/*")
public class FileServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);
    private final FileService fileService = new FileService();

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
            if ("/upload".equals(path)) {
                handleUpload(req, resp, currentUser);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private void handleUpload(HttpServletRequest req, HttpServletResponse resp, User currentUser) throws Exception {
        if (!JakartaServletFileUpload.isMultipartContent(req)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Ожидается multipart/form-data");
            return;
        }

        DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
        JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload = new JakartaServletFileUpload<>(factory);
        upload.setSizeMax(com.cloudstorage.config.AppConfig.getMaxFileSize());

        List<DiskFileItem> items = upload.parseRequest(req);
        Integer folderId = null;
        FileItem uploadedFile = null;

        for (DiskFileItem item : items) {
            if (item.isFormField()) {
                if ("parentFolderId".equals(item.getFieldName())) {
                    String value = item.getString();
                    if (value != null && !value.isEmpty()) {
                        folderId = Integer.parseInt(value);
                    }
                }
            } else {
                try (InputStream inputStream = item.getInputStream()) {
                    String fileName = item.getName();
                    String contentType = item.getContentType();
                    long size = item.getSize();

                    uploadedFile = fileService.uploadFile(
                            currentUser.getId(),
                            folderId,
                            inputStream,
                            fileName,
                            contentType,
                            size
                    );
                }
            }
        }

        if (uploadedFile != null) {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtils.toJson(uploadedFile));
        } else {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Файл не загружен");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);

        try {
            if ("/thumbnail".equals(path)) {
                handleGetThumbnail(req, resp, currentUser);
            } else if ("/download".equals(path)) {
                handleDownload(req, resp, currentUser);
            } else if (path == null || "/".equals(path)) {
                handleListFiles(req, resp, currentUser);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private void handleDownload(HttpServletRequest req, HttpServletResponse resp, User currentUser) throws IOException {
        String fileIdStr = req.getParameter("id");
        if (fileIdStr == null || fileIdStr.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Необходим параметр id");
            return;
        }

        int fileId = Integer.parseInt(fileIdStr);
        FileItem file = fileService.getFileWithAccessCheck(fileId, currentUser != null ? currentUser.getId() : null);

        Path filePath = fileService.getFilePath(file.getUserId(), file.getStorageName());
        if (!Files.exists(filePath)) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Файл не найден на диске");
            return;
        }

        resp.setContentType(file.getMimeType());
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + file.getOriginalName() + "\"");
        resp.setContentLengthLong(file.getSize());

        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(filePath, out);
        }
    }

    private void handleGetThumbnail(HttpServletRequest req, HttpServletResponse resp, User currentUser) throws IOException {
        String fileIdStr = req.getParameter("id");
        if (fileIdStr == null || fileIdStr.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Необходим параметр id");
            return;
        }

        int fileId = Integer.parseInt(fileIdStr);
        FileItem file = fileService.getFileWithAccessCheck(fileId, currentUser != null ? currentUser.getId() : null);

        Path thumbnailPath = fileService.getThumbnailPath(file.getUserId(), file.getStorageName());
        if (!Files.exists(thumbnailPath)) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Миниатюра не найдена");
            return;
        }

        resp.setContentType("image/jpeg");
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(thumbnailPath, out);
        }
    }

    private void handleListFiles(HttpServletRequest req, HttpServletResponse resp, User currentUser) throws IOException {
        if (currentUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            return;
        }

        String folderIdStr = req.getParameter("folderId");
        Integer folderId = null;
        if (folderIdStr != null && !folderIdStr.isEmpty()) {
            folderId = Integer.parseInt(folderIdStr);
        }

        List<FileItem> files = fileService.getFolderFiles(currentUser.getId(), folderId);
        resp.setContentType("application/json");
        resp.getWriter().write(JsonUtils.toJson(files));
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
            String fileIdStr = req.getParameter("id");
            if (fileIdStr == null || fileIdStr.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Необходим параметр id");
                return;
            }

            int fileId = Integer.parseInt(fileIdStr);
            fileService.deleteFile(fileId, currentUser.getId());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtils.toJson(Map.of("message", "Файл удален")));
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
            logger.error("Ошибка при работе с файлами: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtils.errorJson("Внутренняя ошибка сервера"));
        }
    }
}
