CREATE TABLE meetings_consilium (
                                    id SERIAL PRIMARY KEY,
                                    sender_doctor_id INTEGER NOT NULL,     -- доктор-инициатор
                                    receiver_doctor_id INTEGER NOT NULL,   -- второй доктор
                                    room_id VARCHAR(50) UNIQUE NOT NULL,
                                    meeting_url TEXT NOT NULL,
                                    topic VARCHAR(200),
                                    status VARCHAR(20),                    -- scheduled / started / finished / cancelled
                                    scheduled_time TIMESTAMP,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    duration_minutes INTEGER DEFAULT 30,
                                    notes TEXT
);