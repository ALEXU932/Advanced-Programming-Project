-- ============================================================
--  AI-Enhanced Electric Billing System - Complete Database Schema
--  Matches ER Diagram: Admin, Customer, Meter, Reading,
--  Payment, Report, AI Features, Usage Log, User
-- ============================================================

CREATE DATABASE IF NOT EXISTS electric_billing_db;
USE electric_billing_db;

-- ============================================================
-- 1. USERS  (UserID, Username, Password, Role)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    profile_pic   VARCHAR(500) DEFAULT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add profile_pic column if upgrading existing database
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_pic VARCHAR(500) DEFAULT NULL;

-- ============================================================
-- 2. ADMINS  (AdminID, Role) — linked to users
-- ============================================================
CREATE TABLE IF NOT EXISTS admins (
    admin_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL UNIQUE,
    role       VARCHAR(50) DEFAULT 'ADMIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- 3. CUSTOMERS  (CustomerID, Name, Address, Phone)
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    phone       VARCHAR(20),
    address     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- ============================================================
-- 4. METERS  (MeterID, MeterType, Status, Location)
--    Assigned To → Customer
-- ============================================================
CREATE TABLE IF NOT EXISTS meters (
    meter_id     INT AUTO_INCREMENT PRIMARY KEY,
    meter_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id  INT,
    admin_id     INT,
    meter_type   ENUM('SINGLE_PHASE','THREE_PHASE','SMART','PREPAID') DEFAULT 'SINGLE_PHASE',
    status       ENUM('ACTIVE','INACTIVE','FAULTY','REPLACED') DEFAULT 'ACTIVE',
    location     VARCHAR(255),
    installed_at DATE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL,
    FOREIGN KEY (admin_id)    REFERENCES admins(admin_id) ON DELETE SET NULL
);

-- ============================================================
-- 5. TARIFFS  (rate structure for billing)
-- ============================================================
CREATE TABLE IF NOT EXISTS tariffs (
    tariff_id    INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    rate_per_kwh DECIMAL(10,4) NOT NULL,
    fixed_charge DECIMAL(10,2) DEFAULT 0.00,
    start_date   DATE NOT NULL,
    end_date     DATE,
    is_active    BOOLEAN DEFAULT TRUE
);

-- ============================================================
-- 6. READINGS  (ReadingID, ReadingDate, Units)
--    Has → Meter
-- ============================================================
CREATE TABLE IF NOT EXISTS meter_readings (
    reading_id       INT AUTO_INCREMENT PRIMARY KEY,
    meter_id         INT,
    customer_id      INT NOT NULL,
    reading_date     DATE NOT NULL,
    units            DECIMAL(10,2) NOT NULL COMMENT 'Same as consumption_kwh',
    consumption_kwh  DECIMAL(10,2) NOT NULL,
    previous_reading DECIMAL(10,2) DEFAULT 0,
    current_reading  DECIMAL(10,2) DEFAULT 0,
    recorded_by      INT COMMENT 'admin_id who recorded this',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meter_id)    REFERENCES meters(meter_id) ON DELETE SET NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (recorded_by) REFERENCES admins(admin_id) ON DELETE SET NULL
);

-- ============================================================
-- 7. BILLS  (billing records)
-- ============================================================
CREATE TABLE IF NOT EXISTS bills (
    bill_id         INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    meter_id        INT,
    tariff_id       INT NOT NULL,
    billing_month   VARCHAR(7) NOT NULL,
    consumption_kwh DECIMAL(10,2) NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    fixed_charge    DECIMAL(10,2) DEFAULT 0,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          ENUM('PENDING','PAID','OVERDUE') DEFAULT 'PENDING',
    due_date        DATE,
    paid_date       DATE,
    generated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (meter_id)    REFERENCES meters(meter_id) ON DELETE SET NULL,
    FOREIGN KEY (tariff_id)   REFERENCES tariffs(tariff_id)
);

-- ============================================================
-- 8. PAYMENTS  (PaymentID, PaymentDate, Amount, PaymentMethod)
--    Paid By → Reading/Bill
-- ============================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    bill_id        INT NOT NULL,
    customer_id    INT NOT NULL,
    payment_date   DATETIME DEFAULT CURRENT_TIMESTAMP,
    amount         DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH','BANK_TRANSFER','MOBILE_MONEY','CARD','ONLINE') DEFAULT 'CASH',
    reference_no   VARCHAR(100),
    notes          TEXT,
    received_by    INT COMMENT 'admin_id',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id)     REFERENCES bills(bill_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (received_by) REFERENCES admins(admin_id) ON DELETE SET NULL
);

-- ============================================================
-- 9. AI FEATURES  (PredictionID, UsageForecast, FraudAlert, Recommendations)
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_features (
    feature_id      INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    prediction_month VARCHAR(7) NOT NULL,
    usage_forecast  DECIMAL(10,2) COMMENT 'Predicted kWh next month',
    actual_kwh      DECIMAL(10,2) COMMENT 'Filled in after month ends',
    confidence      DECIMAL(5,2)  COMMENT 'Prediction confidence 0-100%',
    fraud_alert     BOOLEAN DEFAULT FALSE,
    fraud_score     DECIMAL(5,2)  DEFAULT 0 COMMENT 'Anomaly z-score',
    recommendations TEXT          COMMENT 'AI energy saving tips',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- ============================================================
-- 10. USAGE LOG  (LogID, Usage, AnomalyDetected)
--     Monitors → Reading/Progress
-- ============================================================
CREATE TABLE IF NOT EXISTS usage_log (
    log_id           INT AUTO_INCREMENT PRIMARY KEY,
    customer_id      INT NOT NULL,
    reading_id       INT,
    log_date         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_kwh        DECIMAL(10,2) NOT NULL,
    anomaly_detected BOOLEAN DEFAULT FALSE,
    anomaly_score    DECIMAL(6,3)  DEFAULT 0 COMMENT 'Z-score value',
    severity         ENUM('NONE','LOW','MEDIUM','HIGH') DEFAULT 'NONE',
    description      TEXT,
    is_resolved      BOOLEAN DEFAULT FALSE,
    resolved_by      INT COMMENT 'admin_id',
    resolved_at      TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (reading_id)  REFERENCES meter_readings(reading_id) ON DELETE SET NULL,
    FOREIGN KEY (resolved_by) REFERENCES admins(admin_id) ON DELETE SET NULL
);

-- ============================================================
-- 11. REPORTS  (ReportID, ReportType, Date)
--     Generated by Admin
-- ============================================================
CREATE TABLE IF NOT EXISTS reports (
    report_id   INT AUTO_INCREMENT PRIMARY KEY,
    admin_id    INT,
    report_type ENUM('MONTHLY_BILLING','CONSUMPTION','PAYMENT_STATUS',
                     'ANOMALY','TARIFF_USAGE','CUSTOMER_SUMMARY') NOT NULL,
    report_date DATE NOT NULL,
    parameters  TEXT  COMMENT 'JSON: month, filters used',
    file_path   VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admins(admin_id) ON DELETE SET NULL
);

-- ============================================================
-- KEEP: ai_predictions (legacy — maps to ai_features)
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_predictions (
    prediction_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    prediction_month VARCHAR(7) NOT NULL,
    predicted_kwh   DECIMAL(10,2) NOT NULL,
    actual_kwh      DECIMAL(10,2),
    confidence      DECIMAL(5,2),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- ============================================================
-- KEEP: anomalies (legacy — maps to usage_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS anomalies (
    anomaly_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id  INT NOT NULL,
    reading_id   INT,
    detected_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description  TEXT,
    severity     ENUM('LOW','MEDIUM','HIGH') DEFAULT 'MEDIUM',
    is_resolved  BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- ============================================================
-- DEFAULT DATA
-- ============================================================

-- Admin user (password: admin123)
INSERT IGNORE INTO users (username, password_hash, role) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN');

-- Admin profile record
INSERT IGNORE INTO admins (user_id, role)
SELECT user_id, 'ADMIN' FROM users WHERE username = 'admin';

-- Default active tariff
INSERT IGNORE INTO tariffs (name, rate_per_kwh, fixed_charge, start_date, is_active) VALUES
('Standard Rate 2024', 0.1200, 5.00, '2024-01-01', TRUE),
('Economy Rate',       0.0900, 3.00, '2024-01-01', FALSE),
('Commercial Rate',    0.1500, 10.00,'2024-01-01', FALSE);

-- Sample customer users (password: admin123)
INSERT IGNORE INTO users (username, password_hash, role) VALUES
('customer1', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'CUSTOMER'),
('customer2', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'CUSTOMER');

-- Sample customer profiles
INSERT IGNORE INTO customers (user_id, name, email, phone, address)
SELECT user_id, 'John Doe',  'john@example.com',  '555-0101', '123 Main St'
FROM users WHERE username = 'customer1';

INSERT IGNORE INTO customers (user_id, name, email, phone, address)
SELECT user_id, 'Jane Smith','jane@example.com',  '555-0102', '456 Oak Ave'
FROM users WHERE username = 'customer2';

-- Sample meters
INSERT IGNORE INTO meters (meter_number, customer_id, meter_type, status, location)
SELECT 'MTR-001001', c.customer_id, 'SMART', 'ACTIVE', '123 Main St'
FROM customers c WHERE c.name = 'John Doe';

INSERT IGNORE INTO meters (meter_number, customer_id, meter_type, status, location)
SELECT 'MTR-001002', c.customer_id, 'SINGLE_PHASE', 'ACTIVE', '456 Oak Ave'
FROM customers c WHERE c.name = 'Jane Smith';
