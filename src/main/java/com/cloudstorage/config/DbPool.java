package com.cloudstorage.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Пул соединений с базой данных через HikariCP
 */
public class DbPool {
    private static final Logger logger = LoggerFactory.getLogger(DbPool.class);
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            // Настройки подключения
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/cloudstorage");
            config.setUsername("postgres");
            config.setPassword("1111");
            config.setDriverClassName("org.postgresql.Driver");

            // Настройки пула
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("CloudStoragePool");

            // Проверка соединения при старте
            config.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(config);

            // Проверка доступности БД
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Успешное подключение к базе данных PostgreSQL");
            }
        } catch (Exception e) {
            logger.error("Ошибка подключения к базе данных: {}", e.getMessage());
            throw new RuntimeException("Не удалось подключиться к базе данных. Приложение остановлено.", e);
        }
    }

    private DbPool() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Пул соединений закрыт");
        }
    }
}
