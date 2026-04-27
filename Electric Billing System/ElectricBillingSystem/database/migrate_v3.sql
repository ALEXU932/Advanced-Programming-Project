USE electric_billing_db;

-- ============================================================
-- Migration v3 — Customer Portal Feature Additions
-- ============================================================

-- Customer notifications table
CREATE TABLE IF NOT EXISTS customer_notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    message         TEXT NOT NULL,
    type            VARCHAR(50) DEFAULT 'INFO',
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Customer auto-pay settings
CREATE TABLE IF NOT EXISTS customer_autopay (
    autopay_id      INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL UNIQUE,
    is_enabled      BOOLEAN DEFAULT FALSE,
    payment_method  VARCHAR(50) DEFAULT 'BANK_TRANSFER',
    reference_info  VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Support tickets
CREATE TABLE IF NOT EXISTS support_tickets (
    ticket_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    type        VARCHAR(50) NOT NULL,
    subject     VARCHAR(200) NOT NULL,
    description TEXT,
    priority    ENUM('LOW','MEDIUM','HIGH','URGENT') DEFAULT 'MEDIUM',
    status      ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED') DEFAULT 'OPEN',
    response    TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

SELECT 'Migration v3 complete' AS result;
