package com.cloudstorage.dao;

import com.cloudstorage.config.DbPool;
import com.cloudstorage.model.Folder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с папками
 */
public class FolderDao {

    private static final String SELECT_BY_ID = "SELECT id, user_id, name, parent_id, deleted, created_at FROM folders WHERE id = ? AND deleted = FALSE";
    private static final String SELECT_BY_USER = "SELECT id, user_id, name, parent_id, deleted, created_at FROM folders WHERE user_id = ? AND deleted = FALSE ORDER BY name";
    private static final String SELECT_BY_USER_AND_PARENT = "SELECT id, user_id, name, parent_id, deleted, created_at FROM folders WHERE user_id = ? AND parent_id IS ? AND deleted = FALSE ORDER BY name";
    private static final String INSERT = "INSERT INTO folders (user_id, name, parent_id) VALUES (?, ?, ?) RETURNING id";
    private static final String SOFT_DELETE = "UPDATE folders SET deleted = TRUE WHERE id = ? AND user_id = ?";
    private static final String SOFT_DELETE_CHILDREN = "UPDATE folders SET deleted = TRUE WHERE parent_id = ? AND user_id = ?";
    private static final String CHECK_DUPLICATE = "SELECT id FROM folders WHERE user_id = ? AND name = ? AND parent_id IS ? AND deleted = FALSE";

    private Folder mapRow(ResultSet rs) throws SQLException {
        Folder folder = new Folder();
        folder.setId(rs.getInt("id"));
        folder.setUserId(rs.getInt("user_id"));
        folder.setName(rs.getString("name"));
        folder.setParentId(rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null);
        folder.setDeleted(rs.getBoolean("deleted"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            folder.setCreatedAt(ts.toInstant());
        }
        return folder;
    }

    public Optional<Folder> findById(int id) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска папки", e);
        }
        return Optional.empty();
    }

    public List<Folder> findByUserId(int userId) {
        List<Folder> folders = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USER)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                folders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения папок пользователя", e);
        }
        return folders;
    }

    public List<Folder> findByUserAndParent(int userId, Integer parentId) {
        List<Folder> folders = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USER_AND_PARENT)) {
            stmt.setInt(1, userId);
            if (parentId == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, parentId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                folders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения папок", e);
        }
        return folders;
    }

    public Folder create(Folder folder) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, folder.getUserId());
            stmt.setString(2, folder.getName());
            if (folder.getParentId() == null) {
                stmt.setNull(3, Types.INTEGER);
            } else {
                stmt.setInt(3, folder.getParentId());
            }
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                folder.setId(rs.getInt(1));
            }
            return folder;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания папки", e);
        }
    }

    public void softDelete(int folderId, int userId) {
        try (Connection conn = DbPool.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Помечаем дочерние папки
                try (PreparedStatement stmt = conn.prepareStatement(SOFT_DELETE_CHILDREN)) {
                    stmt.setInt(1, folderId);
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }
                // Помечаем текущую папку
                try (PreparedStatement stmt = conn.prepareStatement(SOFT_DELETE)) {
                    stmt.setInt(1, folderId);
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления папки", e);
        }
    }

    public boolean existsByNameAndParent(int userId, String name, Integer parentId) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_DUPLICATE)) {
            stmt.setInt(1, userId);
            stmt.setString(2, name);
            if (parentId == null) {
                stmt.setNull(3, Types.INTEGER);
            } else {
                stmt.setInt(3, parentId);
            }
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки дубликата папки", e);
        }
    }
}
