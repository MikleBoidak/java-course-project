package com.cloudstorage.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Слушатель контекста для инициализации приложения
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Инициализация приложения Cloud Storage...");
        
        try {
            // Проверка подключения к БД (уже выполняется в статическом блоке DbPool)
            logger.info("Подключение к базе данных проверено");
            
            // Инициализация директорий хранилища
            AppConfig.initStorage();
            logger.info("Директории хранилища созданы: {}", AppConfig.getUserStoragePath(0).getParent().getParent());
            
            logger.info("Приложение Cloud Storage успешно инициализировано");
        } catch (Exception e) {
            logger.error("Критическая ошибка при инициализации приложения: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать приложение", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Завершение работы приложения Cloud Storage...");
        DbPool.close();
        logger.info("Приложение Cloud Storage остановлено");
    }
}
