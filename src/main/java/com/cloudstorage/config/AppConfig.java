package com.cloudstorage.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Конфигурация приложения
 */
public class AppConfig {
    // Базовая директория для хранения файлов (вне webapps для безопасности)
    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), "cloud-storage-data");
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100 MB

    private AppConfig() {}

    /**
     * Получить путь к директории пользователя
     */
    public static Path getUserStoragePath(int userId) {
        return STORAGE_PATH.resolve("user-files").resolve(String.valueOf(userId));
    }

    /**
     * Получить путь к директории миниатюр пользователя
     */
    public static Path getUserThumbnailPath(int userId) {
        return STORAGE_PATH.resolve("thumbnails").resolve(String.valueOf(userId));
    }

    /**
     * Максимальный размер файла в байтах
     */
    public static long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Инициализация директорий
     */
    public static void initStorage() {
        try {
            java.nio.file.Files.createDirectories(STORAGE_PATH.resolve("user-files"));
            java.nio.file.Files.createDirectories(STORAGE_PATH.resolve("thumbnails"));
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать директории хранилища", e);
        }
    }
}
