package com.cloudstorage.service;

import com.cloudstorage.model.PhotoMetadata;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

/**
 * Сервис чтения EXIF метаданных фотографий
 */
public class ExifService {
    private static final Logger logger = LoggerFactory.getLogger(ExifService.class);

    /**
     * Извлечь EXIF метаданные из изображения
     * @param filePath путь к файлу
     * @param fileId ID файла в БД
     * @return объект PhotoMetadata или null если не удалось извлечь
     */
    public PhotoMetadata extractMetadata(Path filePath, Integer fileId) {
        try {
            File file = filePath.toFile();
            if (!file.exists()) {
                logger.warn("Файл не найден для извлечения EXIF: {}", filePath);
                return null;
            }

            Metadata metadata = ImageMetadataReader.readMetadata(file);
            PhotoMetadata photoMeta = new PhotoMetadata();
            photoMeta.setFileId(fileId);

            // Дата съемки из ExifSubIFDDirectory
            ExifSubIFDDirectory subDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subDir != null) {
                Date dateTaken = subDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dateTaken != null) {
                    photoMeta.setDateTaken(dateTaken.toInstant());
                }
            }

            // Если дата не найдена в ExifSubIFD, пробуем ExifIFD0
            if (photoMeta.getDateTaken() == null) {
                ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (ifd0Dir != null) {
                    Date dateTaken = ifd0Dir.getDate(ExifIFD0Directory.TAG_DATETIME);
                    if (dateTaken != null) {
                        photoMeta.setDateTaken(dateTaken.toInstant());
                    }
                }
            }

            // Модель камеры
            ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Dir != null) {
                String cameraModel = ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL);
                if (cameraModel != null && !cameraModel.isEmpty()) {
                    photoMeta.setCameraModel(cameraModel.trim());
                }
            }

            // GPS координаты
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                GeoLocation geoLocation = gpsDir.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    photoMeta.setLatitude(geoLocation.getLatitude());
                    photoMeta.setLongitude(geoLocation.getLongitude());
                }
            }

            logger.info("EXIF метаданные извлечены для файла {}", fileId);
            return photoMeta;

        } catch (Exception e) {
            logger.error("Ошибка извлечения EXIF метаданных: {}", e.getMessage());
            return null;
        }
    }
}
