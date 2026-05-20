package com.cloudstorage.servlet;

import com.cloudstorage.model.FileItem;
import com.cloudstorage.model.PhotoMetadata;
import com.cloudstorage.model.User;
import com.cloudstorage.service.FileService;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сервлет галереи фотографий
 * GET /api/photos - список всех фотографий пользователя с метаданными
 */
@WebServlet("/api/photos")
public class PhotoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PhotoServlet.class);
    private final FileService fileService = new FileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = SessionUtils.getCurrentUser(req);
        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write(JsonUtils.errorJson("Требуется аутентификация"));
            return;
        }

        try {
            List<PhotoDto> photos = getPhotosWithMetadata(currentUser.getId());
            resp.getWriter().write(JsonUtils.toJson(photos));
        } catch (Exception e) {
            logger.error("Ошибка получения фотографий: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtils.errorJson("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Получить фотографии с метаданными, отсортированные по date_taken DESC
     */
    private List<PhotoDto> getPhotosWithMetadata(int userId) throws Exception {
        List<PhotoDto> photos = new ArrayList<>();
        
        String sql = """
            SELECT f.id, f.original_name, f.storage_name, f.mime_type, f.size, f.created_at,
                   p.date_taken, p.camera_model, p.latitude, p.longitude
            FROM file_items f
            LEFT JOIN photo_metadata p ON f.id = p.file_id
            WHERE f.user_id = ? AND f.mime_type LIKE 'image/%' AND f.deleted = FALSE
            ORDER BY COALESCE(p.date_taken, f.created_at) DESC
            """;

        try (Connection conn = com.cloudstorage.config.DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PhotoDto photo = new PhotoDto();
                photo.setId(rs.getInt("id"));
                photo.setOriginalName(rs.getString("original_name"));
                photo.setStorageName(rs.getString("storage_name"));
                photo.setMimeType(rs.getString("mime_type"));
                photo.setSize(rs.getLong("size"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) photo.setCreatedAt(createdAt.toInstant());
                
                Timestamp dateTaken = rs.getTimestamp("date_taken");
                if (dateTaken != null) photo.setDateTaken(dateTaken.toInstant());
                
                photo.setCameraModel(rs.getString("camera_model"));
                photo.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
                photo.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);
                
                // URL превью
                photo.setThumbnailUrl("/api/files/thumbnail?id=" + photo.getId());
                
                photos.add(photo);
            }
        }
        
        return photos;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "GET");
        resp.getWriter().write(JsonUtils.errorJson("Method not allowed"));
    }

    /**
     * DTO для ответа с фотографиями
     */
    public static class PhotoDto {
        private Integer id;
        private String originalName;
        private String storageName;
        private String mimeType;
        private long size;
        private java.time.Instant createdAt;
        private java.time.Instant dateTaken;
        private String cameraModel;
        private Double latitude;
        private Double longitude;
        private String thumbnailUrl;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        public String getStorageName() { return storageName; }
        public void setStorageName(String storageName) { this.storageName = storageName; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public java.time.Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
        public java.time.Instant getDateTaken() { return dateTaken; }
        public void setDateTaken(java.time.Instant dateTaken) { this.dateTaken = dateTaken; }
        public String getCameraModel() { return cameraModel; }
        public void setCameraModel(String cameraModel) { this.cameraModel = cameraModel; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    }
}
