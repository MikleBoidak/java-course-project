-- Скрипт инициализации базы данных для Облачного хранилища
-- PostgreSQL 14+

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     login VARCHAR(50) NOT NULL UNIQUE,
                                     email VARCHAR(100) NOT NULL UNIQUE,
                                     password_hash VARCHAR(255) NOT NULL,
                                     role VARCHAR(20) NOT NULL DEFAULT 'USER',
                                     quota BIGINT NOT NULL DEFAULT 1073741824,
                                     used_space BIGINT NOT NULL DEFAULT 0,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица папок
CREATE TABLE IF NOT EXISTS folders (
                                       id SERIAL PRIMARY KEY,
                                       user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       name VARCHAR(255) NOT NULL,
                                       parent_id INTEGER REFERENCES folders(id) ON DELETE CASCADE,
                                       deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                       UNIQUE (user_id, name, parent_id)
);

-- Таблица файлов (название file_items)
CREATE TABLE IF NOT EXISTS file_items (
                                          id SERIAL PRIMARY KEY,
                                          user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL,
                                          original_name VARCHAR(255) NOT NULL,
                                          storage_name VARCHAR(255) NOT NULL,
                                          mime_type VARCHAR(100),
                                          size BIGINT NOT NULL DEFAULT 0,
                                          deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица метаданных фотографий
CREATE TABLE IF NOT EXISTS photo_metadata (
                                              id SERIAL PRIMARY KEY,
                                              file_id INTEGER NOT NULL REFERENCES file_items(id) ON DELETE CASCADE,
                                              date_taken TIMESTAMP WITH TIME ZONE,
                                              camera_model VARCHAR(255),
                                              latitude DOUBLE PRECISION,
                                              longitude DOUBLE PRECISION,
                                              UNIQUE (file_id)
);

-- Таблица публичных ссылок
CREATE TABLE IF NOT EXISTS public_shares (
                                             id SERIAL PRIMARY KEY,
                                             token VARCHAR(255) NOT NULL UNIQUE,
                                             item_id INTEGER NOT NULL,
                                             item_type VARCHAR(10) NOT NULL CHECK (item_type IN ('FILE', 'FOLDER')),
                                             user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             expires_at TIMESTAMP WITH TIME ZONE,
                                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица шаринга конкретным пользователям
CREATE TABLE IF NOT EXISTS user_shares (
                                           id SERIAL PRIMARY KEY,
                                           item_id INTEGER NOT NULL,
                                           item_type VARCHAR(10) NOT NULL CHECK (item_type IN ('FILE', 'FOLDER')),
                                           owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           target_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           permission VARCHAR(10) NOT NULL DEFAULT 'READ' CHECK (permission IN ('READ', 'WRITE')),
                                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                           UNIQUE (item_id, item_type, target_user_id)
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_folders_user_id ON folders(user_id);
CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);
CREATE INDEX IF NOT EXISTS idx_file_items_user_id ON file_items(user_id);
CREATE INDEX IF NOT EXISTS idx_file_items_folder_id ON file_items(folder_id);
CREATE INDEX IF NOT EXISTS idx_file_items_mime_type ON file_items(mime_type);
CREATE INDEX IF NOT EXISTS idx_file_items_deleted ON file_items(deleted);
CREATE INDEX IF NOT EXISTS idx_public_shares_token ON public_shares(token);
CREATE INDEX IF NOT EXISTS idx_user_shares_target ON user_shares(target_user_id);
CREATE INDEX IF NOT EXISTS idx_user_shares_item ON user_shares(item_id, item_type);
CREATE INDEX IF NOT EXISTS idx_photo_metadata_file ON photo_metadata(file_id);

-- Триггеры для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_file_items_updated_at ON file_items;
CREATE TRIGGER update_file_items_updated_at
    BEFORE UPDATE ON file_items
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_folders_updated_at ON folders;
CREATE TRIGGER update_folders_updated_at
    BEFORE UPDATE ON folders
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Создание администратора (пароль: admin123) – выполнить вручную
-- INSERT INTO users (login, email, password_hash, role, quota, used_space)
-- VALUES ('admin', 'admin@cloud.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 10737418240, 0);