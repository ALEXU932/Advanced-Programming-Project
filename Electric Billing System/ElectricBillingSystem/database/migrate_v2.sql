USE electric_billing_db;

-- disputes table (Feature 9)
CREATE TABLE IF NOT EXISTS disputes (
    dispute_id   INT AUTO_INCREMENT PRIMARY KEY,
    bill_id      INT NOT NULL,
    customer_id  INT NOT NULL,
    reason       VARCHAR(100) NOT NULL,
    description  TEXT,
    status       ENUM('OPEN','UNDER_REVIEW','RESOLVED','REJECTED') DEFAULT 'OPEN',
    resolution   TEXT,
    adjusted_amount DECIMAL(10,2),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at  TIMESTAMP NULL,
    resolved_by  INT,
    FOREIGN KEY (bill_id)     REFERENCES bills(bill_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- customer_budgets table (Feature 8)
CREATE TABLE IF NOT EXISTS customer_budgets (
    budget_id        INT AUTO_INCREMENT PRIMARY KEY,
    customer_id      INT NOT NULL UNIQUE,
    monthly_budget_kwh DECIMAL(10,2) NOT NULL,
    alert_threshold  INT DEFAULT 80,
    is_active        BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- SMTP settings defaults
INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES
('smtp_host',     'smtp.gmail.com'),
('smtp_port',     '587'),
('smtp_username', ''),
('smtp_password', ''),
('smtp_from_name','Electric Billing System'),
('smtp_enabled',  'false'),
('smtp_tls',      'true');

-- Add dispute action to audit log enum (handled in Java)
SELECT 'Migration v2 complete' AS result;
