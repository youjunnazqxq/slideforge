CREATE TABLE deck_drafts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    initial_prompt TEXT NOT NULL,
    outline_json TEXT,
    sticky_notes_json TEXT,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_deck_drafts_user_id ON deck_drafts(user_id);
