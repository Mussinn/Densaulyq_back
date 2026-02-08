-- Таблица чатов
CREATE TABLE chats (
                       chat_id BIGSERIAL PRIMARY KEY,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица участников чата (связь многие ко многим между users и chats)
CREATE TABLE chat_participants (
                                   id BIGSERIAL PRIMARY KEY,
                                   chat_id BIGINT NOT NULL REFERENCES chats(chat_id) ON DELETE CASCADE,
                                   user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                   joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   last_read_at TIMESTAMP,
                                   UNIQUE(chat_id, user_id)
);

-- Индексы для быстрого поиска
CREATE INDEX idx_chat_participants_chat_id ON chat_participants(chat_id);
CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);

-- Таблица сообщений
CREATE TABLE messages (
                          message_id BIGSERIAL PRIMARY KEY,
                          chat_id BIGINT NOT NULL REFERENCES chats(chat_id) ON DELETE CASCADE,
                          sender_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                          content TEXT,
                          reply_to_id BIGINT REFERENCES messages(message_id) ON DELETE SET NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP,
                          read_at TIMESTAMP,
                          is_deleted BOOLEAN DEFAULT FALSE
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_reply_to_id ON messages(reply_to_id);

-- Таблица вложений
CREATE TABLE message_attachments (
                                     attachment_id BIGSERIAL PRIMARY KEY,
                                     message_id BIGINT NOT NULL REFERENCES messages(message_id) ON DELETE CASCADE,
                                     file_name VARCHAR(255) NOT NULL,
                                     file_path VARCHAR(500) NOT NULL,
                                     file_type VARCHAR(100),
                                     file_size BIGINT,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для быстрого поиска вложений по сообщению
CREATE INDEX idx_attachments_message_id ON message_attachments(message_id);

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггеры для автоматического обновления updated_at
CREATE TRIGGER update_chats_updated_at BEFORE UPDATE ON chats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_messages_updated_at BEFORE UPDATE ON messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Представление для получения последнего сообщения в каждом чате
CREATE OR REPLACE VIEW chat_last_messages AS
SELECT DISTINCT ON (chat_id)
    chat_id,
    message_id,
    sender_id,
    content,
    created_at
FROM messages
WHERE is_deleted = FALSE
ORDER BY chat_id, created_at DESC;

-- Представление для подсчета непрочитанных сообщений
CREATE OR REPLACE VIEW unread_messages_count AS
SELECT
    cp.user_id,
    cp.chat_id,
    COUNT(m.message_id) as unread_count
FROM chat_participants cp
         LEFT JOIN messages m ON m.chat_id = cp.chat_id
    AND m.sender_id != cp.user_id
    AND m.created_at > COALESCE(cp.last_read_at, '1970-01-01'::timestamp)
    AND m.is_deleted = FALSE
GROUP BY cp.user_id, cp.chat_id;