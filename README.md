# Облачное хранилище файлов и фотографий

Веб-приложение для хранения файлов и фотографий с поддержкой общего доступа.

## Технологический стек

- **Сервер приложений**: Tomcat 11.0+ (Jakarta EE 10, Servlet 6.0)
- **База данных**: PostgreSQL 18+
- **Java**: JDK 25
- **Сборка**: Maven

## Зависимости

- Jakarta Servlet API 6.0
- PostgreSQL Driver 42.7.4
- HikariCP 6.2.1 (пул соединений)
- Jackson 2.18.2 (JSON)
- Commons FileUpload 2.0.0-M2 (загрузка файлов)
- Thumbnailator 0.4.20 (миниатюры)
- Metadata Extractor 2.19.0 (EXIF)
- jBCrypt 0.10.2 (хеширование паролей)
- SLF4J 2.0.16 (логирование)

## Быстрый старт

### 1. Настройка базы данных

```sql
-- Создайте базу данных
CREATE DATABASE cloudstorage;

-- Создайте пользователя (опционально)
CREATE USER clouduser WITH PASSWORD 'cloudpass';
GRANT ALL PRIVILEGES ON DATABASE cloudstorage TO clouduser;
```

### 2. Настройка подключения к БД

Отредактируйте `src/main/java/com/cloudstorage/config/DbPool.java`:

```java
config.setJdbcUrl("jdbc:postgresql://localhost:5432/cloudstorage");
config.setUsername("postgres");
config.setPassword("postgres");
```

### 3. Инициализация схемы БД

Выполните SQL-скрипт:

```bash
psql -U postgres -d cloudstorage -f src/main/resources/db/migration/init.sql
```

### 4. Создание администратора

```sql
INSERT INTO users (login, email, password_hash, role, quota, used_space) 
VALUES ('admin', 'admin@cloud.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 10737418240, 0);
-- Пароль: admin123
```

### 5. Сборка приложения

```bash
mvn clean package
```

### 6. Развертывание в Tomcat

1. Скопируйте `target/cloud-storage.war` в `TOMCAT_HOME/webapps/`
2. Запустите Tomcat: `TOMCAT_HOME/bin/startup.sh` (или `startup.bat` на Windows)
3. Откройте `http://localhost:8080/cloud-storage/`

## API Endpoints

### Аутентификация
- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход
- `POST /api/auth/logout` - Выход
- `GET /api/auth/check` - Проверка сессии

### Файлы
- `POST /api/files/upload` - Загрузка файла (multipart/form-data)
- `GET /api/files/download?id={fileId}` - Скачивание
- `GET /api/files/thumbnail?id={fileId}` - Миниатюра
- `GET /api/files?folderId={folderId}` - Список файлов
- `DELETE /api/files?id={fileId}` - Удаление

### Папки
- `POST /api/folders` - Создание папки
- `GET /api/folders/tree` - Дерево папок
- `DELETE /api/folders?id={folderId}` - Удаление

### Фотографии
- `GET /api/photos` - Галерея фотографий с EXIF

### Общий доступ
- `POST /api/shares/public` - Публичная ссылка
- `POST /api/shares/user` - Доступ пользователю
- `GET /shared/{token}` - Публичный доступ

### Поиск
- `GET /api/search?query={text}&type={type}` - Поиск файлов

### Администрирование
- `GET /api/admin/users` - Список пользователей (ADMIN)
- `PUT /api/admin/users/{userId}/quota` - Изменение квоты (ADMIN)

## Структура проекта

```
src/
├── main/
│   ├── java/com/cloudstorage/
│   │   ├── config/          # Конфигурация (DbPool, AppConfig)
│   │   ├── model/           # Модели данных
│   │   ├── dao/             # DAO слой
│   │   ├── service/         # Бизнес-логика
│   │   ├── servlet/         # REST API сервлеты
│   │   ├── filter/          # Фильтры
│   │   ├── util/            # Утилиты
│   │   └── exception/       # Исключения
│   ├── resources/
│   │   └── db/migration/    # SQL скрипты
│   └── webapp/
│       ├── WEB-INF/web.xml  # Конфигурация веб-приложения
│       ├── index.html       # Frontend
│       ├── css/style.css    # Стили
│       └── js/app.js        # JavaScript
└── test/                    # Тесты
```

## Хранение файлов

Файлы хранятся вне директории webapps для безопасности:
- `${user.home}/cloud-storage-data/user-files/{userId}/` - файлы пользователей
- `${user.home}/cloud-storage-data/thumbnails/{userId}/` - миниатюры

## Безопасность

- Хеширование паролей через BCrypt
- Защита от XSS (экранирование вывода)
- Проверка сессии для защищенных эндпоинтов
- Soft delete для файлов и папок
- Проверка квот дискового пространства

## Лимиты

- Максимальный размер файла: 100 MB (настраивается в `AppConfig.java`)
- Квота по умолчанию: 1 GB на пользователя

## Лицензия

Проект создан в образовательных целях.
