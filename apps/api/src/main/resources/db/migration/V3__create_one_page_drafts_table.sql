CREATE TABLE IF NOT EXISTS one_page_drafts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    initial_prompt TEXT NOT NULL,
    conversation_json TEXT,
    requirement_brief_json TEXT,
    research_pack_json TEXT,
    page_plan_json TEXT,
    visual_spec_json TEXT,
    svg_content TEXT,
    validation_report_json TEXT,
    status VARCHAR(64) NOT NULL,
    failed_stage VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

