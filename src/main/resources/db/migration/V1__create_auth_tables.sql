CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR NOT NULL UNIQUE,
    username VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    role_id BIGINT NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_users_role_id
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user_id
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS email_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    verification_code TEXT NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_email_verifications_user_id
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
    ON refresh_tokens (token);

CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id
    ON email_verifications (user_id);

CREATE INDEX IF NOT EXISTS idx_email_verifications_verification_code
    ON email_verifications (verification_code);
