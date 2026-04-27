-- ============================================================
--  Migration Script — Run this on your EXISTING database
--  to update it to the new schema without losing data
-- ============================================================
USE electric_billing_db;

-- 1. Add profile_pic to users if missing
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_pic VARCHAR(500) DEFAULT NULL;

-- 2. Create meters table if it doesn't exist
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
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
);

-- 3. Migrate existing meter_number from customers → meters table
-- (only if customers table still has meter_number column)
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'electric_billing_db'
    AND TABLE_NAME = 'customers'
    AND COLUMN_NAME = 'meter_number'
);

-- Migrate data if column exists
INSERT IGNORE INTO meters (meter_number, customer_id, meter_type, status)
SELECT meter_number, customer_id, 'SINGLE_PHASE', 'ACTIVE'
FROM customers
WHERE meter_number IS NOT NULL AND meter_number != ''
AND (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA='electric_billing_db'
     AND TABLE_NAME='customers' AND COLUMN_NAME='meter_number') > 0;

-- 4. Drop old meter_number column from customers (safe — data migrated above)
ALTER TABLE customers DROP COLUMN IF EXISTS meter_number;

-- 5. Create admins table if missing
CREATE TABLE IF NOT EXISTS admins (
    admin_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL UNIQUE,
    role       VARCHAR(50) DEFAULT 'ADMIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Populate admins table from users
INSERT IGNORE INTO admins (user_id, role)
SELECT user_id, 'ADMIN' FROM users WHERE role = 'ADMIN';

-- 6. Create payments table if missing
CREATE TABLE IF NOT EXISTS payments (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    bill_id        INT NOT NULL,
    customer_id    INT NOT NULL,
    payment_date   DATETIME DEFAULT CURRENT_TIMESTAMP,
    amount         DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH','BANK_TRANSFER','MOBILE_MONEY','CARD','ONLINE') DEFAULT 'CASH',
    reference_no   VARCHAR(100),
    notes          TEXT,
    received_by    INT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id)     REFERENCES bills(bill_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- 7. Create usage_log table if missing
CREATE TABLE IF NOT EXISTS usage_log (
    log_id           INT AUTO_INCREMENT PRIMARY KEY,
    customer_id      INT NOT NULL,
    reading_id       INT,
    log_date         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_kwh        DECIMAL(10,2) NOT NULL,
    anomaly_detected BOOLEAN DEFAULT FALSE,
    anomaly_score    DECIMAL(6,3) DEFAULT 0,
    severity         ENUM('NONE','LOW','MEDIUM','HIGH') DEFAULT 'NONE',
    description      TEXT,
    is_resolved      BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- 8. Create reports table if missing
CREATE TABLE IF NOT EXISTS reports (
    report_id   INT AUTO_INCREMENT PRIMARY KEY,
    admin_id    INT,
    report_type ENUM('MONTHLY_BILLING','CONSUMPTION','PAYMENT_STATUS',
                     'ANOMALY','TARIFF_USAGE','CUSTOMER_SUMMARY') NOT NULL,
    report_date DATE NOT NULL,
    parameters  TEXT,
    file_path   VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Create ai_features table if missing
CREATE TABLE IF NOT EXISTS ai_features (
    feature_id       INT AUTO_INCREMENT PRIMARY KEY,
    customer_id      INT NOT NULL,
    prediction_month VARCHAR(7) NOT NULL,
    usage_forecast   DECIMAL(10,2),
    actual_kwh       DECIMAL(10,2),
    confidence       DECIMAL(5,2),
    fraud_alert      BOOLEAN DEFAULT FALSE,
    fraud_score      DECIMAL(5,2) DEFAULT 0,
    recommendations  TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Add meter_id to meter_readings if missing
ALTER TABLE meter_readings ADD COLUMN IF NOT EXISTS meter_id INT DEFAULT NULL;
ALTER TABLE meter_readings ADD COLUMN IF NOT EXISTS recorded_by INT DEFAULT NULL;

-- Add meter_id to bills if missing
ALTER TABLE bills ADD COLUMN IF NOT EXISTS meter_id INT DEFAULT NULL;

SELECT 'Migration completed successfully!' AS result;
