package com.cloudstorage.service;

import com.cloudstorage.dao.UserDao;
import com.cloudstorage.exception.QuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис управления квотами дискового пространства
 */
public class QuotaService {
    private static final Logger logger = LoggerFactory.getLogger(QuotaService.class);
    private final UserDao userDao;

    public QuotaService() {
        this.userDao = new UserDao();
    }

    /**
     * Проверить, достаточно ли места для загрузки файла
     */
    public void checkQuota(int userId, long fileSize) {
        var userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            throw new QuotaExceededException("Пользователь не найден");
        }

        var user = userOpt.get();
        long newUsedSpace = user.getUsedSpace() + fileSize;

        if (newUsedSpace > user.getQuota()) {
            long available = user.getQuota() - user.getUsedSpace();
            throw new QuotaExceededException(
                    String.format("Недостаточно места. Доступно: %d байт, требуется: %d байт", 
                            available, fileSize));
        }
    }

    /**
     * Увеличить использованное пространство
     */
    public void addUsedSpace(int userId, long size) {
        userDao.updateUsedSpace(userId, size);
        logger.debug("Увеличено использованное пространство пользователя {} на {} байт", userId, size);
    }

    /**
     * Уменьшить использованное пространство
     */
    public void removeUsedSpace(int userId, long size) {
        userDao.updateUsedSpace(userId, -size);
        logger.debug("Уменьшено использованное пространство пользователя {} на {} байт", userId, size);
    }

    /**
     * Получить доступное пространство пользователя
     */
    public long getAvailableSpace(int userId) {
        return userDao.findById(userId)
                .map(user -> user.getQuota() - user.getUsedSpace())
                .orElse(0L);
    }
}
