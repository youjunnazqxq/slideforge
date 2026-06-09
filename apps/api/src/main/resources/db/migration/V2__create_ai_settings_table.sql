CREATE TABLE IF NOT EXISTS ai_settings (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    provider VARCHAR(64) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    encrypted_api_key TEXT,
    api_key_mask VARCHAR(64),
    model VARCHAR(128) NOT NULL,
    temperature DOUBLE PRECISION,
    max_tokens INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

