package com.cloudstorage.dao;

import com.cloudstorage.config.DbPool;
import com.cloudstorage.model.Folder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FolderDao {

    public Optional<Folder> findById(int id) {
        String sql = "SELECT id, user_id, name, parent_id, deleted, created_at FROM folders WHERE id = ? AND deleted = FALSE";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        String sql = "SELECT id, user_id, name, parent_id, deleted, created_at FROM folders WHERE user_id = ? AND deleted = FALSE ORDER BY name";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        String sql = "SELECT id, user_id, name, parent_id, deleted, created_at FROM folders WHERE user_id = ? AND parent_id IS NOT DISTINCT FROM ? AND deleted = FALSE ORDER BY name";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setObject(2, parentId, Types.INTEGER);
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
        String sql = "INSERT INTO folders (user_id, name, parent_id) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, folder.getUserId());
            stmt.setString(2, folder.getName());
            stmt.setObject(3, folder.getParentId(), Types.INTEGER);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                folder.setId(rs.getInt(1));
            }
            return folder;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания папки", e);
        }
    }

    public void softDelete(int folderId, int userId) {
        String deleteSelf = "UPDATE folders SET deleted = TRUE WHERE id = ? AND user_id = ?";
        String deleteChildren = "UPDATE folders SET deleted = TRUE WHERE parent_id = ? AND user_id = ?";
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmtChildren = conn.prepareStatement(deleteChildren);
                 PreparedStatement stmtSelf = conn.prepareStatement(deleteSelf)) {
                stmtChildren.setInt(1, folderId);
                stmtChildren.setInt(2, userId);
                stmtChildren.executeUpdate();
                stmtSelf.setInt(1, folderId);
                stmtSelf.setInt(2, userId);
                stmtSelf.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // ignore rollback error
                }
            }
            throw new RuntimeException("Ошибка удаления папки", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // ignore close error
                }
            }
        }
    }

    public boolean existsByNameAndParent(int userId, String name, Integer parentId) {
        String sql = "SELECT id FROM folders WHERE user_id = ? AND name = ? AND parent_id IS NOT DISTINCT FROM ? AND deleted = FALSE";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, name);
            stmt.setObject(3, parentId, Types.INTEGER);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки дубликата папки", e);
        }
    }

    private Folder mapRow(ResultSet rs) throws SQLException {
        Folder folder = new Folder();
        folder.setId(rs.getInt("id"));
        folder.setUserId(rs.getInt("user_id"));
        folder.setName(rs.getString("name"));
        int parentId = rs.getInt("parent_id");
        folder.setParentId(rs.wasNull() ? null : parentId);
        folder.setDeleted(rs.getBoolean("deleted"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) folder.setCreatedAt(ts.toInstant());
        return folder;
    }
}