CREATE DATABASE IF NOT EXISTS paysentry;
USE paysentry;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    trusted_devices TEXT NULL,
    trusted_locations TEXT NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(120) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS sms_scans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message_content TEXT NOT NULL,
    suspicious_keywords TEXT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    explanation TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_sms_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS qr_scans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NULL,
    extracted_content TEXT NULL,
    extracted_url VARCHAR(500) NULL,
    findings TEXT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    explanation TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_qr_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS url_scans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    findings TEXT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    explanation TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_url_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    receiver VARCHAR(160) NOT NULL,
    location VARCHAR(120) NOT NULL,
    device VARCHAR(160) NOT NULL,
    transaction_time DATETIME NOT NULL,
    flags TEXT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    explanation TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scan_type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    reference_id BIGINT NOT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    account_id VARCHAR(40) NOT NULL UNIQUE,
    total_balance DECIMAL(14, 2) NOT NULL,
    available_balance DECIMAL(14, 2) NOT NULL,
    lien_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS monitored_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    external_transaction_id VARCHAR(80) NOT NULL UNIQUE,
    amount DECIMAL(14, 2) NOT NULL,
    receiver VARCHAR(160) NOT NULL,
    location VARCHAR(120) NOT NULL,
    device VARCHAR(160) NOT NULL,
    new_device BOOLEAN NOT NULL DEFAULT FALSE,
    new_location BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_time DATETIME NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(12) NOT NULL,
    reasons TEXT NOT NULL,
    CONSTRAINT fk_monitored_transaction_account FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);

CREATE TABLE IF NOT EXISTS lien_changes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    reference_id VARCHAR(80) NOT NULL UNIQUE,
    previous_lien_amount DECIMAL(14, 2) NOT NULL,
    lien_amount DECIMAL(14, 2) NOT NULL,
    lien_reason VARCHAR(300) NOT NULL,
    expected_activity BOOLEAN NOT NULL DEFAULT FALSE,
    changed_at DATETIME NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(12) NOT NULL,
    reasons TEXT NOT NULL,
    CONSTRAINT fk_lien_change_account FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);

CREATE TABLE IF NOT EXISTS fraud_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    external_event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(30) NOT NULL,
    direct_risk_score INT NOT NULL,
    overall_risk_score INT NOT NULL,
    risk_level VARCHAR(12) NOT NULL,
    reasons TEXT NOT NULL,
    event_details TEXT NOT NULL,
    occurred_at DATETIME NOT NULL,
    CONSTRAINT fk_fraud_event_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_fraud_event_account FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    fraud_event_id BIGINT NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(12) NOT NULL,
    reasons TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at DATETIME NOT NULL,
    acknowledged_at DATETIME NULL,
    CONSTRAINT fk_fraud_alert_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_fraud_alert_account FOREIGN KEY (account_id) REFERENCES bank_accounts(id),
    CONSTRAINT fk_fraud_alert_event FOREIGN KEY (fraud_event_id) REFERENCES fraud_events(id)
);

CREATE TABLE IF NOT EXISTS mobile_device_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(500) NOT NULL UNIQUE,
    device_name VARCHAR(160) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);
