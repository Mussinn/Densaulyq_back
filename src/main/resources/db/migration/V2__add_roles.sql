INSERT INTO roles (role_name) VALUES
('ROLE_ADMIN'),
('ROLE_DOCTOR'),
('ROLE_USER');

-- 2. Заполнение таблицы users
INSERT INTO users (username, password_hash, email, first_name, last_name, created_at, updated_at) VALUES
('admin1', '$2a$12$xQXgPi2V2lTmaWn.dgrDTeEN7E7dWfeDql9agaLpIrvBWtVSR4Fbu', 'admin1@example.com', 'Азамат', 'Каримов', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor1', '$2a$12$gA1X25Gwsgy5pIIaun1Lfub.16Bkcj26.cf5/Aw.qNYCvjVuVIFN2', 'doctor1@example.com', 'Бауыржан', 'Сериков', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor2', '$2a$12$TuZTCgyAwBFjUYHIGapSIeAT4D.LetMyVzSkPAFJ1VjDPbTrRnRi6', 'doctor2@example.com', 'Гульнара', 'Ахметова', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient1', '$2a$12$ubDRKoS./iGu4mbzUTXRZuTxCDJjTJZsVCf9Ed7o/YGPU6G2.EoXS', 'patient1@example.com', 'Данияр', 'Тулеев', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient2', '$2a$12$bXTRnh1haQXozKudN1NUk.ToYgoP.V9aCpbWI36Fk95KWRx0YixPS', 'patient2@example.com', 'Еркежан', 'Муратова', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient3', '$2a$12$lr0r855./AhRjoMzOPqeq./7zIQK6eACeFeZvLL9te/tox9rvcG5u', 'patient3@example.com', 'Жанар', 'Касымова', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient4', '$2a$12$nYr5HIQj46GFVJWb5x3qYeFbvYRE9nlJ7pRbi/faFnswhM9zAJiN6', 'patient4@example.com', 'Кайрат', 'Беков', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient5', '$2a$12$2ZKi548z69anuwyGpXX5Je7GT/4.9EW6qVZa7.9EIFMdo1k6dG4SC', 'patient5@example.com', 'Ляззат', 'Сулейменова', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor3', '$2a$12$XvbLZfVBVV/rIGpj7fKhHOHxFsUpCnO0DHwqpujkU4kNtwdS0vIfW', 'doctor3@example.com', 'Марат', 'Ибрагимов', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient6', '$2a$12$qgQecG2MlVbNIVA/.VsgEu8JNJgbEVMik.iEtL2TfJfa/ixCT45Mu', 'patient6@example.com', 'Нурлан', 'Алиев', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Заполнение таблицы user_roles
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- admin1 -> admin
(2, 2), -- doctor1 -> doctor
(3, 2), -- doctor2 -> doctor
(4, 3), -- patient1 -> patient
(5, 3), -- patient2 -> patient
(6, 3), -- patient3 -> patient
(7, 3), -- patient4 -> patient
(8, 3), -- patient5 -> patient
(9, 2), -- doctor3 -> doctor
(10, 3); -- patient6 -> patient

-- 4. Заполнение таблицы patients
INSERT INTO patients (user_id, date_of_birth, gender, contact_number, address, created_at) VALUES
(4, '1985-03-15', 'male', '+77011234567', 'Алматы, ул. Абая, 10', CURRENT_TIMESTAMP),
(5, '1990-07-22', 'female', '+77021234568', 'Астана, ул. Сейфуллина, 5', CURRENT_TIMESTAMP),
(6, '1978-11-30', 'female', '+77031234569', 'Шымкент, ул. Тауке хана, 20', CURRENT_TIMESTAMP),
(7, '1995-01-10', 'male', '+77041234570', 'Алматы, ул. Жибек Жолы, 15', CURRENT_TIMESTAMP),
(8, '1988-06-05', 'female', '+77051234571', 'Астана, ул. Кенесары, 8', CURRENT_TIMESTAMP),
(10, '1983-09-12', 'male', '+77061234572', 'Караганда, ул. Ерубаева, 12', CURRENT_TIMESTAMP);

-- 5. Заполнение таблицы doctors
INSERT INTO doctors (user_id, specialty, contact_number, created_at) VALUES
(2, 'Кардиолог', '+77012345678', CURRENT_TIMESTAMP),
(3, 'Терапевт', '+77022345679', CURRENT_TIMESTAMP),
(9, 'Невролог', '+77032345680', CURRENT_TIMESTAMP);

-- 6. Заполнение таблицы medical_records
INSERT INTO medical_records (patient_id, created_at, updated_at) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 8. Заполнение таблицы appointments
INSERT INTO appointments (patient_id, doctor_id, appointment_date, status, created_at) VALUES
(1, 1, '2025-05-16 10:00:00', 'scheduled', CURRENT_TIMESTAMP),
(2, 2, '2025-05-16 11:00:00', 'scheduled', CURRENT_TIMESTAMP),
(3, 3, '2025-05-17 09:30:00', 'scheduled', CURRENT_TIMESTAMP),
(4, 1, '2025-05-17 14:00:00', 'completed', CURRENT_TIMESTAMP),
(5, 2, '2025-05-18 15:00:00', 'scheduled', CURRENT_TIMESTAMP),
(6, 3, '2025-05-18 16:30:00', 'cancelled', CURRENT_TIMESTAMP);

-- 9. Заполнение таблицы medications
INSERT INTO medications (name, dosage, instructions) VALUES
('Парацетамол', '500 мг', 'Принимать 1 таблетку каждые 6 часов при температуре'),
('Амлодипин', '5 мг', 'Принимать 1 таблетку утром при гипертонии'),
('Омепразол', '20 мг', 'Принимать 1 капсулу утром натощак'),
('Ибупрофен', '400 мг', 'Принимать 1 таблетку при болях, не чаще 3 раз в день'),
('Метформин', '500 мг', 'Принимать 1 таблетку 2 раза в день после еды');

-- 10. Заполнение таблицы prescriptions
INSERT INTO prescriptions (patient_id, doctor_id, medication_id, prescription_date, dosage, instructions) VALUES
(1, 1, 2, CURRENT_TIMESTAMP, '5 мг', '1 таблетка утром'),
(2, 2, 1, CURRENT_TIMESTAMP, '500 мг', '1 таблетка при температуре'),
(3, 3, 3, CURRENT_TIMESTAMP, '20 мг', '1 капсула утром'),
(4, 1, 4, CURRENT_TIMESTAMP, '400 мг', '1 таблетка при мигрени'),
(5, 2, 5, CURRENT_TIMESTAMP, '500 мг', '1 таблетка 2 раза в день');

-- 11. Заполнение таблицы tests
INSERT INTO tests (patient_id, test_name, test_date, result, created_at) VALUES
(1, 'Общий анализ крови', '2025-05-01 08:00:00', 'Гемоглобин: 140 г/л', CURRENT_TIMESTAMP),
(2, 'Анализ мочи', '2025-05-02 09:00:00', 'Норма', CURRENT_TIMESTAMP),
(3, 'ЭКГ', '2025-05-03 10:00:00', 'Без патологий', CURRENT_TIMESTAMP),
(4, 'МРТ головы', '2025-05-04 11:00:00', 'Признаки мигрени', CURRENT_TIMESTAMP),
(5, 'Рентген легких', '2025-05-05 12:00:00', 'Признаки бронхита', CURRENT_TIMESTAMP);

-- 12. Заполнение таблицы audit_log
INSERT INTO audit_log (user_id, action, target_table, target_id, action_date, ip_address) VALUES
(1, 'Создание диагноза', 'diagnoses', 1, CURRENT_TIMESTAMP, '192.168.1.1'),
(2, 'Назначение рецепта', 'prescriptions', 1, CURRENT_TIMESTAMP, '192.168.1.2'),
(3, 'Создание записи', 'medical_records', 1, CURRENT_TIMESTAMP, '192.168.1.3'),
(4, 'Проведение теста', 'tests', 1, CURRENT_TIMESTAMP, '192.168.1.4'),
(5, 'Создание записи', 'appointments', 1, CURRENT_TIMESTAMP, '192.168.1.5');

-- 13. Заполнение таблицы security_keys
INSERT INTO security_keys (user_id, public_key, private_key, created_at) VALUES
(2, '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----', '-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBA...\n-----END PRIVATE KEY-----', CURRENT_TIMESTAMP),
(3, '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----', '-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBA...\n-----END PRIVATE KEY-----', CURRENT_TIMESTAMP),
(9, '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----', '-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBA...\n-----END PRIVATE KEY-----', CURRENT_TIMESTAMP);