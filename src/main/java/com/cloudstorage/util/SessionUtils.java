package com.cloudstorage.util;

import com.cloudstorage.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Утилиты для работы с HTTP сессией
 */
public class SessionUtils {
    private static final String USER_ATTR = "user";

    private SessionUtils() {}

    /**
     * Получить текущего пользователя из сессии
     */
    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(USER_ATTR);
    }

    /**
     * Установить пользователя в сессию
     */
    public static void setCurrentUser(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        // Не сохраняем пароль в сессии
        User sessionUser = new User(user.getId(), user.getLogin(), user.getEmail(),
                user.getRole(), user.getQuota(), user.getUsedSpace());
        session.setAttribute(USER_ATTR, sessionUser);
    }

    /**
     * Очистить сессию
     */
    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Проверить, аутентифицирован ли пользователь
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }
}
