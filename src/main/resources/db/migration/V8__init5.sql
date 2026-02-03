CREATE TABLE meetings (
                          id SERIAL PRIMARY KEY,
                          appointment_id INTEGER,
                          doctor_id INTEGER NOT NULL,
                          patient_id INTEGER,
                          room_id VARCHAR(50) UNIQUE NOT NULL,
                          meeting_url TEXT NOT NULL,
                          topic VARCHAR(200),
                          patient_email VARCHAR(255),
                          status VARCHAR(20),
                          scheduled_time TIMESTAMP,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          duration_minutes INTEGER DEFAULT 30,
                          notes TEXT
);