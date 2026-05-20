package com.cloudstorage.dao;

import com.cloudstorage.config.DbPool;
import com.cloudstorage.model.FileItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с файлами
 */
public class FileDao {

    private static final String SELECT_BY_ID = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE id = ? AND deleted = FALSE";
    private static final String SELECT_BY_USER = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE user_id = ? AND deleted = FALSE ORDER BY created_at DESC";
    private static final String SELECT_BY_FOLDER = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE user_id = ? AND folder_id IS ? AND deleted = FALSE ORDER BY created_at DESC";
    private static final String INSERT = "INSERT INTO files (user_id, folder_id, original_name, storage_name, mime_type, size) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
    private static final String SOFT_DELETE = "UPDATE files SET deleted = TRUE WHERE id = ? AND user_id = ?";
    private static final String SELECT_IMAGES = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE user_id = ? AND mime_type LIKE 'image/%' AND deleted = FALSE ORDER BY created_at DESC";
    private static final String SEARCH = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE user_id = ? AND original_name ILIKE ? AND deleted = FALSE ORDER BY created_at DESC";
    private static final String SEARCH_BY_TYPE = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE user_id = ? AND original_name ILIKE ? AND mime_type LIKE ? AND deleted = FALSE ORDER BY created_at DESC";
    private static final String SELECT_BY_IDS = "SELECT id, user_id, folder_id, original_name, storage_name, mime_type, size, deleted, created_at, updated_at FROM files WHERE id = ANY(?) AND deleted = FALSE";

    private FileItem mapRow(ResultSet rs) throws SQLException {
        FileItem file = new FileItem();
        file.setId(rs.getInt("id"));
        file.setUserId(rs.getInt("user_id"));
        file.setFolderId(rs.getObject("folder_id") != null ? rs.getInt("folder_id") : null);
        file.setOriginalName(rs.getString("original_name"));
        file.setStorageName(rs.getString("storage_name"));
        file.setMimeType(rs.getString("mime_type"));
        file.setSize(rs.getLong("size"));
        file.setDeleted(rs.getBoolean("deleted"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) file.setCreatedAt(ts.toInstant());
        Timestamp tsUpd = rs.getTimestamp("updated_at");
        if (tsUpd != null) file.setUpdatedAt(tsUpd.toInstant());
        return file;
    }

    public Optional<FileItem> findById(int id) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска файла", e);
        }
        return Optional.empty();
    }

    public List<FileItem> findByUserId(int userId) {
        List<FileItem> files = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USER)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения файлов пользователя", e);
        }
        return files;
    }

    public List<FileItem> findByFolder(int userId, Integer folderId) {
        List<FileItem> files = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_FOLDER)) {
            stmt.setInt(1, userId);
            if (folderId == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, folderId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения файлов папки", e);
        }
        return files;
    }

    public FileItem create(FileItem file) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, file.getUserId());
            if (file.getFolderId() == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, file.getFolderId());
            }
            stmt.setString(3, file.getOriginalName());
            stmt.setString(4, file.getStorageName());
            stmt.setString(5, file.getMimeType());
            stmt.setLong(6, file.getSize());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                file.setId(rs.getInt(1));
            }
            return file;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания файла", e);
        }
    }

    public void softDelete(int fileId, int userId) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SOFT_DELETE)) {
            stmt.setInt(1, fileId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления файла", e);
        }
    }

    public List<FileItem> findImagesByUser(int userId) {
        List<FileItem> files = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_IMAGES)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения изображений", e);
        }
        return files;
    }

    public List<FileItem> search(int userId, String query) {
        List<FileItem> files = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH)) {
            stmt.setInt(1, userId);
            stmt.setString(2, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска файлов", e);
        }
        return files;
    }

    public List<FileItem> searchByType(int userId, String query, String mimePattern) {
        List<FileItem> files = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_TYPE)) {
            stmt.setInt(1, userId);
            stmt.setString(2, "%" + query + "%");
            stmt.setString(3, mimePattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска файлов по типу", e);
        }
        return files;
    }

    public List<FileItem> findByFolderIds(List<Integer> folderIds) {
        List<FileItem> files = new ArrayList<>();
        if (folderIds.isEmpty()) return files;
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_IDS)) {
            Array array = conn.createArrayOf("INTEGER", folderIds.toArray());
            stmt.setArray(1, array);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения файлов по папкам", e);
        }
        return files;
    }
}
