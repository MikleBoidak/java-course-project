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
