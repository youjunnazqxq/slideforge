CREATE TABLE IF NOT EXISTS workflow_runs (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    draft_id UUID,
    stage VARCHAR(64) NOT NULL,
    model VARCHAR(128),
    prompt_key VARCHAR(128),
    prompt_version VARCHAR(32),
    input_json TEXT,
    output_json TEXT,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL
);

