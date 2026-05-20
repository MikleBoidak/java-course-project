package com.cloudstorage.service;

import com.cloudstorage.config.AppConfig;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сервис генерации миниатюр изображений
 */
public class ThumbnailService {
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);
    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;

    /**
     * Сгенерировать миниатюру для изображения
     * @param userId ID пользователя
     * @param storageName UUID имя файла на диске
     * @return имя файла миниатюры или null если не удалось
     */
    public String generateThumbnail(int userId, String storageName) {
        try {
            Path sourcePath = AppConfig.getUserStoragePath(userId).resolve(storageName);
            if (!Files.exists(sourcePath)) {
                logger.warn("Исходный файл не найден для миниатюры: {}", sourcePath);
                return null;
            }

            // Создаем директорию для миниатюр если нужно
            Path thumbnailDir = AppConfig.getUserThumbnailPath(userId);
            Files.createDirectories(thumbnailDir);

            String thumbnailName = storageName + ".jpg";
            Path thumbnailPath = thumbnailDir.resolve(thumbnailName);

            BufferedImage originalImage = ImageIO.read(sourcePath.toFile());
            if (originalImage == null) {
                logger.warn("Не удалось прочитать изображение: {}", sourcePath);
                return null;
            }

            Thumbnails.of(originalImage)
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .outputFormat("jpg")
                    .toFile(thumbnailPath.toFile());

            logger.info("Миниатюра создана: {}", thumbnailPath);
            return thumbnailName;

        } catch (IOException e) {
            logger.error("Ошибка генерации миниатюры для {}: {}", storageName, e.getMessage());
            return null;
        }
    }

    /**
     * Получить путь к миниатюре
     */
    public static Path getThumbnailPath(int userId, String thumbnailName) {
        return AppConfig.getUserThumbnailPath(userId).resolve(thumbnailName);
    }

    /**
     * Удалить миниатюру
     */
    public void deleteThumbnail(int userId, String thumbnailName) {
        try {
            Path path = getThumbnailPath(userId, thumbnailName);
            Files.deleteIfExists(path);
            logger.debug("Миниатюра удалена: {}", path);
        } catch (IOException e) {
            logger.error("Ошибка удаления миниатюры: {}", e.getMessage());
        }
    }
}
