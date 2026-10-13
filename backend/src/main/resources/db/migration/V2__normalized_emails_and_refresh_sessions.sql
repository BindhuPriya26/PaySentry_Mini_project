ALTER TABLE users
    ADD COLUMN normalized_email VARCHAR(160) NULL;

UPDATE users
SET email = LOWER(TRIM(email));

UPDATE users
SET normalized_email = LOWER(TRIM(email));

ALTER TABLE users
    MODIFY normalized_email VARCHAR(160) NOT NULL;

CREATE UNIQUE INDEX uq_users_normalized_email ON users (normalized_email);

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    session_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    token_version INT NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_refresh_tokens_expires_at (expires_at),
    INDEX idx_refresh_tokens_session_active (session_id, expires_at),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);
