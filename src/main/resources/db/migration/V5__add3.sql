ALTER TABLE users ADD COLUMN online BOOLEAN;

CREATE TABLE call_history (
                              id BIGSERIAL PRIMARY KEY,
                              call_id VARCHAR(255) UNIQUE NOT NULL,
                              caller_id BIGINT NOT NULL,
                              receiver_id BIGINT NOT NULL,
                              call_type VARCHAR(50) NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              duration BIGINT DEFAULT 0,
                              notes VARCHAR(500),
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_call_history_caller FOREIGN KEY (caller_id) REFERENCES users(user_id) ON DELETE CASCADE,
                              CONSTRAINT fk_call_history_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_call_history_caller_id ON call_history(caller_id);
CREATE INDEX idx_call_history_receiver_id ON call_history(receiver_id);
CREATE INDEX idx_call_history_created_at ON call_history(created_at);
CREATE INDEX idx_call_history_status ON call_history(status);

COMMENT ON COLUMN call_history.call_type IS 'PATIENT_TO_DOCTOR, DOCTOR_TO_PATIENT, DOCTOR_TO_DOCTOR';
COMMENT ON COLUMN call_history.status IS 'ACCEPTED, REJECTED, MISSED, ENDED_BY_CALLER, ENDED_BY_RECEIVER, CANCELLED';
COMMENT ON COLUMN call_history.duration IS 'Длительность звонка в секундах';