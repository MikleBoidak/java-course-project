package com.cloudstorage.service;

import com.cloudstorage.dao.UserDao;
import com.cloudstorage.model.User;
import com.cloudstorage.exception.AppException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис аутентификации и регистрации пользователей
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDao();
    }

    /**
     * Регистрация нового пользователя
     */
    public User register(String login, String email, String password) {
        // Валидация входных данных
        if (login == null || login.trim().length() < 3) {
            throw new AppException("Логин должен содержать минимум 3 символа", 400);
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new AppException("Некорректный email", 400);
        }
        if (password == null || password.length() < 6) {
            throw new AppException("Пароль должен содержать минимум 6 символов", 400);
        }

        // Проверка существования пользователя
        if (userDao.findByLogin(login).isPresent()) {
            throw new AppException("Пользователь с таким логином уже существует", 409);
        }
        if (userDao.findByEmail(email).isPresent()) {
            throw new AppException("Email уже зарегистрирован", 409);
        }

        // Хеширование пароля
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        User user = new User();
        user.setLogin(login.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordHash);
        user.setRole("USER");
        user.setQuota(1073741824L); // 1 GB по умолчанию
        user.setUsedSpace(0L);

        User created = userDao.create(user);
        logger.info("Зарегистрирован новый пользователь: {}", login);
        return created;
    }

    /**
     * Аутентификация пользователя
     */
    public User login(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            throw new AppException("Введите логин", 400);
        }
        if (password == null || password.isEmpty()) {
            throw new AppException("Введите пароль", 400);
        }

        User user = userDao.findByLogin(login.trim())
                .orElseThrow(() -> new AppException("Неверный логин или пароль", 401));

        // Проверка пароля
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new AppException("Неверный логин или пароль", 401);
        }

        // Возвращаем пользователя без пароля
        user.setPasswordHash(null);
        return user;
    }

    /**
     * Получить пользователя по ID
     */
    public User getUserById(int userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new AppException("Пользователь не найден", 404));
        user.setPasswordHash(null);
        return user;
    }

    /**
     * Обновить квоту пользователя (только для администраторов)
     */
    public void updateQuota(int userId, long quota, int adminId) {
        User admin = userDao.findById(adminId)
                .orElseThrow(() -> new AppException("Администратор не найден", 404));
        
        if (!"ADMIN".equals(admin.getRole())) {
            throw new AppException("Требуется роль администратора", 403);
        }

        if (quota < 0) {
            throw new AppException("Квота не может быть отрицательной", 400);
        }

        userDao.updateQuota(userId, quota);
        logger.info("Администратор {} изменил квоту пользователя {} на {} байт", adminId, userId, quota);
    }
}
