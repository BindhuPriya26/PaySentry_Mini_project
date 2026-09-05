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
