package com.cloudstorage.servlet;

import com.cloudstorage.model.FileItem;
import com.cloudstorage.model.Folder;
import com.cloudstorage.model.PublicShare;
import com.cloudstorage.service.FileService;
import com.cloudstorage.service.ShareService;
import com.cloudstorage.util.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервлет публичного доступа к файлам по ссылке
 * GET /shared/{token} - доступ к файлу или папке по публичной ссылке
 */
@WebServlet("/shared/*")
public class PublicAccessServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(PublicAccessServlet.class);
    private final ShareService shareService = new ShareService();
    private final FileService fileService = new FileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getPathInfo();
        if (token != null && token.startsWith("/")) {
            token = token.substring(1);
        }

        if (token == null || token.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Необходим токен");
            return;
        }

        try {
            Optional<PublicShare> shareOpt = shareService.getPublicShare(token);
            if (shareOpt.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Ссылка не найдена");
                return;
            }

            PublicShare share = shareOpt.get();
            if (share.isExpired()) {
                sendError(resp, HttpServletResponse.SC_GONE, "Ссылка истекла");
                return;
            }

            if ("FILE".equals(share.getItemType())) {
                handleFileAccess(share, req, resp);
            } else {
                handleFolderAccess(share, resp);
            }

        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private void handleFileAccess(PublicShare share, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<FileItem> fileOpt = new com.cloudstorage.dao.FileDao().findById(share.getItemId());
        if (fileOpt.isEmpty() || fileOpt.get().isDeleted()) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Файл не найден");
            return;
        }

        FileItem file = fileOpt.get();
        String download = req.getParameter("download");

        if ("true".equals(download)) {
            // Скачивание файла
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
        } else {
            // Информация о файле
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(JsonUtils.toJson(Map.of(
                    "name", file.getOriginalName(),
                    "size", file.getSize(),
                    "mimeType", file.getMimeType(),
                    "downloadUrl", req.getRequestURI() + "?download=true"
            )));
        }
    }

    private void handleFolderAccess(PublicShare share, HttpServletResponse resp) throws IOException {
        Optional<Folder> folderOpt = new com.cloudstorage.dao.FolderDao().findById(share.getItemId());
        if (folderOpt.isEmpty() || folderOpt.get().isDeleted()) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Папка не найдена");
            return;
        }

        Folder folder = folderOpt.get();
        List<FileItem> files = shareService.getPublicFolderContents(folder.getId());

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(JsonUtils.toJson(Map.of(
                "folderName", folder.getName(),
                "files", files
        )));
    }

}
