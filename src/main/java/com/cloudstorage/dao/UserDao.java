package com.cloudstorage.dao;

import com.cloudstorage.config.DbPool;
import com.cloudstorage.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с пользователями
 */
public class UserDao {

    private static final String SELECT_BY_ID = "SELECT id, login, email, password_hash, role, quota, used_space FROM users WHERE id = ?";
    private static final String SELECT_BY_LOGIN = "SELECT id, login, email, password_hash, role, quota, used_space FROM users WHERE login = ?";
    private static final String SELECT_BY_EMAIL = "SELECT id, login, email, password_hash, role, quota, used_space FROM users WHERE email = ?";
    private static final String INSERT = "INSERT INTO users (login, email, password_hash, role, quota, used_space) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
    private static final String UPDATE_USED_SPACE = "UPDATE users SET used_space = used_space + ? WHERE id = ?";
    private static final String UPDATE_QUOTA = "UPDATE users SET quota = ? WHERE id = ?";
    private static final String SELECT_ALL = "SELECT id, login, email, password_hash, role, quota, used_space FROM users ORDER BY id";

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setQuota(rs.getLong("quota"));
        user.setUsedSpace(rs.getLong("used_space"));
        return user;
    }

    public Optional<User> findById(int id) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по ID", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByLogin(String login) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_LOGIN)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по логину", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_EMAIL)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по email", e);
        }
        return Optional.empty();
    }

    public User create(User user) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole());
            stmt.setLong(5, user.getQuota());
            stmt.setLong(6, user.getUsedSpace());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания пользователя", e);
        }
    }

    public void updateUsedSpace(int userId, long delta) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_USED_SPACE)) {
            stmt.setLong(1, delta);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления использованного пространства", e);
        }
    }

    public void updateQuota(int userId, long quota) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_QUOTA)) {
            stmt.setLong(1, quota);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления квоты", e);
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка пользователей", e);
        }
        return users;
    }
}
