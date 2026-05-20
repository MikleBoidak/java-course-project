package com.cloudstorage.util;

import com.cloudstorage.config.AppConfig;
import com.cloudstorage.exception.AppException;

/**
 * Валидатор файлов
 */
public class FileValidator {

    private FileValidator() {}

    /**
     * Проверить размер файла
     */
    public static void validateFileSize(long size) {
        if (size <= 0) {
            throw new AppException("Файл пустой", 400);
        }
        if (size > AppConfig.getMaxFileSize()) {
            throw new AppException("Файл слишком большой. Максимум " + (AppConfig.getMaxFileSize() / (1024 * 1024)) + " MB", 413);
        }
    }

    /**
     * Проверить имя файла на безопасность
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new AppException("Имя файла не может быть пустым", 400);
        }
        // Удаляем опасные символы
        String sanitized = fileName.replaceAll("[\\x00-\\x1f\\\\/:*?\"<>|]", "_");
        // Удаляем path traversal
        sanitized = sanitized.replaceAll("\\.\\.", "_");
        return sanitized.trim();
    }

    /**
     * Проверить, является ли MIME-тип изображением
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Определить категорию файла по MIME-типу
     */
    public static String getFileCategory(String mimeType) {
        if (mimeType == null) return "unknown";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("text/") || mimeType.contains("pdf") ||
            mimeType.contains("word") || mimeType.contains("excel") ||
            mimeType.contains("powerpoint") || mimeType.contains("office")) {
            return "document";
        }
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        return "other";
    }
}
