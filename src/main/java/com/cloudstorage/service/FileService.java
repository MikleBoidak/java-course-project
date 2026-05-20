package com.cloudstorage.service;

import com.cloudstorage.config.AppConfig;
import com.cloudstorage.dao.FileDao;
import com.cloudstorage.dao.FolderDao;
import com.cloudstorage.dao.ShareDao;
import com.cloudstorage.exception.AppException;
import com.cloudstorage.exception.QuotaExceededException;
import com.cloudstorage.model.FileItem;
import com.cloudstorage.model.Folder;
import com.cloudstorage.model.PhotoMetadata;
import com.cloudstorage.model.UserShare;
import com.cloudstorage.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис управления файлами
 */
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileDao fileDao;
    private final FolderDao folderDao;
    private final ShareDao shareDao;
    private final QuotaService quotaService;
    private final ThumbnailService thumbnailService;
    private final ExifService exifService;

    public FileService() {
        this.fileDao = new FileDao();
        this.folderDao = new FolderDao();
        this.shareDao = new ShareDao();
        this.quotaService = new QuotaService();
        this.thumbnailService = new ThumbnailService();
        this.exifService = new ExifService();
    }

    /**
     * Загрузить файл
     * @param userId ID пользователя
     * @param folderId ID папки (может быть null)
     * @param inputStream поток данных файла
     * @param originalName оригинальное имя файла
     * @param mimeType MIME тип
     * @param fileSize размер файла
     * @return загруженный файл
     */
    public FileItem uploadFile(int userId, Integer folderId, InputStream inputStream, 
                               String originalName, String mimeType, long fileSize) {
        // Валидация размера
        FileValidator.validateFileSize(fileSize);

        // Проверка квоты
        quotaService.checkQuota(userId, fileSize);

        // Проверка папки
        if (folderId != null) {
            Folder folder = folderDao.findById(folderId)
                    .orElseThrow(() -> new AppException("Папка не найдена", 404));
            if (!folder.getUserId().equals(userId)) {
                throw new AppException("Нет доступа к папке", 403);
            }
            if (folder.isDeleted()) {
                throw new AppException("Папка удалена", 400);
            }
        }

        // Санитизация имени файла
        String sanitizedName = FileValidator.sanitizeFileName(originalName);

        // Генерация UUID имени для хранения
        String storageName = UUID.randomUUID().toString();

        // Создание директории пользователя
        Path userDir = AppConfig.getUserStoragePath(userId);
        try {
            Files.createDirectories(userDir);
        } catch (IOException e) {
            logger.error("Ошибка создания директории пользователя: {}", e.getMessage());
            throw new AppException("Ошибка сервера при создании директории", 500);
        }

        // Сохранение файла
        Path filePath = userDir.resolve(storageName);
        try {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Ошибка сохранения файла: {}", e.getMessage());
            throw new AppException("Ошибка сохранения файла", 500);
        }

        // Создание записи в БД
        FileItem file = new FileItem();
        file.setUserId(userId);
        file.setFolderId(folderId);
        file.setOriginalName(sanitizedName);
        file.setStorageName(storageName);
        file.setMimeType(mimeType);
        file.setSize(fileSize);
        file.setDeleted(false);

        FileItem savedFile = fileDao.create(file);

        // Обновление квоты
        quotaService.addUsedSpace(userId, fileSize);

        // Обработка изображений
        if (FileValidator.isImage(mimeType)) {
            // Генерация миниатюры
            String thumbnailName = thumbnailService.generateThumbnail(userId, storageName);
            if (thumbnailName != null) {
                logger.info("Миниатюра создана для файла {}", savedFile.getId());
            }

            // Извлечение EXIF
            PhotoMetadata metadata = exifService.extractMetadata(filePath, savedFile.getId());
            if (metadata != null) {
                // Сохраняем метаданные в БД через DAO (добавим метод)
                savePhotoMetadata(metadata);
            }
        }

        logger.info("Файл загружен: {} ({} байт, {})", sanitizedName, fileSize, mimeType);
        return savedFile;
    }

    /**
     * Сохранить метаданные фотографии в БД
     */
    private void savePhotoMetadata(PhotoMetadata metadata) {
        try (var conn = com.cloudstorage.config.DbPool.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO photo_metadata (file_id, date_taken, camera_model, latitude, longitude) VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (file_id) DO UPDATE SET date_taken = EXCLUDED.date_taken, " +
                     "camera_model = EXCLUDED.camera_model, latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude")) {
            stmt.setInt(1, metadata.getFileId());
            if (metadata.getDateTaken() != null) {
                stmt.setTimestamp(2, java.sql.Timestamp.from(metadata.getDateTaken()));
            } else {
                stmt.setNull(2, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(3, metadata.getCameraModel());
            if (metadata.getLatitude() != null) {
                stmt.setDouble(4, metadata.getLatitude());
            } else {
                stmt.setNull(4, java.sql.Types.DOUBLE);
            }
            if (metadata.getLongitude() != null) {
                stmt.setDouble(5, metadata.getLongitude());
            } else {
                stmt.setNull(5, java.sql.Types.DOUBLE);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Ошибка сохранения метаданных: {}", e.getMessage());
        }
    }

    /**
     * Получить файл по ID с проверкой прав доступа
     */
    public FileItem getFileWithAccessCheck(int fileId, Integer currentUserId) {
        FileItem file = fileDao.findById(fileId)
                .orElseThrow(() -> new AppException("Файл не найден", 404));

        // Проверка прав доступа
        if (!hasAccess(file, currentUserId)) {
            throw new AppException("Нет доступа к файлу", 403);
        }

        return file;
    }

    /**
     * Проверить наличие доступа к файлу
     */
    public boolean hasAccess(FileItem file, Integer currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        // Владелец всегда имеет доступ
        if (file.getUserId().equals(currentUserId)) {
            return true;
        }
        // Проверка user_shares
        Optional<UserShare> share = shareDao.findUserShare(file.getId(), "FILE", currentUserId);
        return share.isPresent();
    }

    /**
     * Удалить файл (soft delete)
     */
    public void deleteFile(int fileId, int userId) {
        FileItem file = fileDao.findById(fileId)
                .orElseThrow(() -> new AppException("Файл не найден", 404));

        if (!file.getUserId().equals(userId)) {
            throw new AppException("Нет доступа к файлу", 403);
        }
        if (file.isDeleted()) {
            throw new AppException("Файл уже удален", 400);
        }

        // Soft delete
        fileDao.softDelete(fileId, userId);

        // Обновление квоты
        quotaService.removeUsedSpace(userId, file.getSize());

        // Удаление миниатюры
        String thumbnailName = file.getStorageName() + ".jpg";
        thumbnailService.deleteThumbnail(userId, thumbnailName);

        logger.info("Файл удален: {} ({} байт)", file.getOriginalName(), file.getSize());
    }

    /**
     * Получить все файлы пользователя
     */
    public List<FileItem> getUserFiles(int userId) {
        return fileDao.findByUserId(userId);
    }

    /**
     * Получить файлы из папки
     */
    public List<FileItem> getFolderFiles(int userId, Integer folderId) {
        return fileDao.findByFolder(userId, folderId);
    }

    /**
     * Получить все изображения пользователя (для галереи)
     */
    public List<FileItem> getUserImages(int userId) {
        return fileDao.findImagesByUser(userId);
    }

    /**
     * Поиск файлов
     */
    public List<FileItem> searchFiles(int userId, String query, String type) {
        if (type == null || type.isEmpty()) {
            return fileDao.search(userId, query);
        }
        String mimePattern;
        switch (type.toLowerCase()) {
            case "image":
                mimePattern = "image/%";
                break;
            case "document":
                mimePattern = "%pdf%";
                break;
            case "video":
                mimePattern = "video/%";
                break;
            case "audio":
                mimePattern = "audio/%";
                break;
            default:
                mimePattern = "%" + type + "%";
        }
        return fileDao.searchByType(userId, query, mimePattern);
    }

    /**
     * Получить путь к файлу на диске
     */
    public Path getFilePath(int userId, String storageName) {
        return AppConfig.getUserStoragePath(userId).resolve(storageName);
    }

    /**
     * Получить путь к миниатюре
     */
    public Path getThumbnailPath(int userId, String storageName) {
        return AppConfig.getUserThumbnailPath(userId).resolve(storageName + ".jpg");
    }
}
