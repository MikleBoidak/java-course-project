package com.cloudstorage.dao;

import com.cloudstorage.config.DbPool;
import com.cloudstorage.model.PublicShare;
import com.cloudstorage.model.UserShare;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с общим доступом
 */
public class ShareDao {

    private static final String INSERT_PUBLIC = "INSERT INTO public_shares (token, item_id, item_type, user_id, expires_at) VALUES (?, ?, ?, ?, ?) RETURNING id";
    private static final String SELECT_PUBLIC_BY_TOKEN = "SELECT id, token, item_id, item_type, user_id, expires_at, created_at FROM public_shares WHERE token = ?";
    private static final String SELECT_PUBLIC_BY_ITEM = "SELECT id, token, item_id, item_type, user_id, expires_at, created_at FROM public_shares WHERE item_id = ? AND item_type = ? AND user_id = ?";
    private static final String INSERT_USER_SHARE = "INSERT INTO user_shares (item_id, item_type, owner_id, target_user_id, permission) VALUES (?, ?, ?, ?, ?) RETURNING id";
    private static final String SELECT_USER_SHARES_BY_ITEM = "SELECT id, item_id, item_type, owner_id, target_user_id, permission, created_at FROM user_shares WHERE item_id = ? AND item_type = ?";
    private static final String SELECT_USER_SHARES_BY_TARGET = "SELECT id, item_id, item_type, owner_id, target_user_id, permission, created_at FROM user_shares WHERE target_user_id = ?";
    private static final String CHECK_USER_SHARE = "SELECT id, item_id, item_type, owner_id, target_user_id, permission, created_at FROM user_shares WHERE item_id = ? AND item_type = ? AND target_user_id = ?";
    private static final String DELETE_PUBLIC = "DELETE FROM public_shares WHERE token = ? AND user_id = ?";

    private PublicShare mapPublicShare(ResultSet rs) throws SQLException {
        PublicShare share = new PublicShare();
        share.setId(rs.getInt("id"));
        share.setToken(rs.getString("token"));
        share.setItemId(rs.getInt("item_id"));
        share.setItemType(rs.getString("item_type"));
        share.setUserId(rs.getInt("user_id"));
        Timestamp exp = rs.getTimestamp("expires_at");
        if (exp != null) share.setExpiresAt(exp.toInstant());
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) share.setCreatedAt(ts.toInstant());
        return share;
    }

    private UserShare mapUserShare(ResultSet rs) throws SQLException {
        UserShare share = new UserShare();
        share.setId(rs.getInt("id"));
        share.setItemId(rs.getInt("item_id"));
        share.setItemType(rs.getString("item_type"));
        share.setOwnerId(rs.getInt("owner_id"));
        share.setTargetUserId(rs.getInt("target_user_id"));
        share.setPermission(rs.getString("permission"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) share.setCreatedAt(ts.toInstant());
        return share;
    }

    public PublicShare createPublicShare(PublicShare share) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_PUBLIC, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, share.getToken());
            stmt.setInt(2, share.getItemId());
            stmt.setString(3, share.getItemType());
            stmt.setInt(4, share.getUserId());
            if (share.getExpiresAt() == null) {
                stmt.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                stmt.setTimestamp(5, Timestamp.from(share.getExpiresAt()));
            }
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                share.setId(rs.getInt(1));
            }
            return share;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания публичной ссылки", e);
        }
    }

    public Optional<PublicShare> findPublicByToken(String token) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PUBLIC_BY_TOKEN)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapPublicShare(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска публичной ссылки", e);
        }
        return Optional.empty();
    }

    public List<PublicShare> findPublicByItem(int itemId, String itemType) {
        List<PublicShare> shares = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PUBLIC_BY_ITEM)) {
            stmt.setInt(1, itemId);
            stmt.setString(2, itemType);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                shares.add(mapPublicShare(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения публичных ссылок", e);
        }
        return shares;
    }

    public UserShare createUserShare(UserShare share) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_USER_SHARE, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, share.getItemId());
            stmt.setString(2, share.getItemType());
            stmt.setInt(3, share.getOwnerId());
            stmt.setInt(4, share.getTargetUserId());
            stmt.setString(5, share.getPermission());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                share.setId(rs.getInt(1));
            }
            return share;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания пользовательского шаринга", e);
        }
    }

    public List<UserShare> findUserSharesByItem(int itemId, String itemType) {
        List<UserShare> shares = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_USER_SHARES_BY_ITEM)) {
            stmt.setInt(1, itemId);
            stmt.setString(2, itemType);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                shares.add(mapUserShare(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения шарингов", e);
        }
        return shares;
    }

    public List<UserShare> findUserSharesByTarget(int targetUserId) {
        List<UserShare> shares = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_USER_SHARES_BY_TARGET)) {
            stmt.setInt(1, targetUserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                shares.add(mapUserShare(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения шарингов для пользователя", e);
        }
        return shares;
    }

    public Optional<UserShare> findUserShare(int itemId, String itemType, int targetUserId) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_USER_SHARE)) {
            stmt.setInt(1, itemId);
            stmt.setString(2, itemType);
            stmt.setInt(3, targetUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapUserShare(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки шаринга", e);
        }
        return Optional.empty();
    }

    public void deletePublicShare(String token, int userId) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_PUBLIC)) {
            stmt.setString(1, token);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления публичной ссылки", e);
        }
    }
}
