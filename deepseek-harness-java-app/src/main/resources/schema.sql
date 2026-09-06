CREATE TABLE IF NOT EXISTS harness_session (
    session_id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(128) NOT NULL,
    profile_code VARCHAR(64) NOT NULL,
    execution_mode VARCHAR(32) NOT NULL,
    resolved_model VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS harness_task (
    task_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    prompt TEXT NOT NULL,
    enabled_tools_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT fk_harness_task_session FOREIGN KEY (session_id) REFERENCES harness_session(session_id)
);

CREATE TABLE IF NOT EXISTS harness_session_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    KEY idx_harness_session_event_session (session_id),
    CONSTRAINT fk_harness_session_event_session FOREIGN KEY (session_id) REFERENCES harness_session(session_id)
);

CREATE TABLE IF NOT EXISTS harness_plugin_installation (
    plugin_id VARCHAR(128) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    plugin_version VARCHAR(64) NOT NULL,
    runtime_type VARCHAR(64) NOT NULL,
    install_mode VARCHAR(64) NOT NULL,
    source_path VARCHAR(512) NOT NULL,
    entrypoint VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    installed_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS harness_plugin_runtime_binding (
    plugin_id VARCHAR(128) PRIMARY KEY,
    package_type VARCHAR(64) NOT NULL,
    manifest_path VARCHAR(512) NULL,
    working_directory VARCHAR(512) NULL,
    runtime_command VARCHAR(1024) NULL,
    bridge_ready TINYINT(1) NOT NULL,
    message TEXT NOT NULL,
    checked_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT fk_harness_plugin_runtime_binding_plugin FOREIGN KEY (plugin_id) REFERENCES harness_plugin_installation(plugin_id)
);

CREATE TABLE IF NOT EXISTS harness_model_setting (
    profile_code VARCHAR(64) PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    base_url VARCHAR(255) NULL,
    api_key_ref VARCHAR(128) NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS harness_goal_state (
    session_id VARCHAR(64) PRIMARY KEY,
    goal_id VARCHAR(128) NOT NULL,
    revision BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL,
    blocked_reason TEXT NULL,
    max_rounds INT NOT NULL,
    rounds_started INT NOT NULL,
    continuation_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);

-- ── New session event system tables ──────────────────────────
CREATE TABLE IF NOT EXISTS harness_session_header (
    session_id VARCHAR(64) PRIMARY KEY,
    version INT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    cwd VARCHAR(512) NULL,
    parent_session VARCHAR(64) NULL,
    seed_length INT NOT NULL DEFAULT 0,
    origin VARCHAR(32) NULL,
    delegation_depth INT NOT NULL DEFAULT 0,
    agent_preset VARCHAR(128) NULL
);

CREATE TABLE IF NOT EXISTS harness_session_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    seq BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_time TIMESTAMP(3) NOT NULL,
    payload_json TEXT NULL,
    surface_op VARCHAR(16) NULL,
    source_event_seqs VARCHAR(512) NULL,
    UNIQUE (session_id, seq),
    KEY idx_harness_session_event_log_session (session_id)
);

INSERT INTO harness_model_setting (profile_code, provider_code, model_code, base_url, api_key_ref, enabled, updated_at)
SELECT 'headless', 'deepseek', 'gpt-5.5', 'http://127.0.0.1:8777/v1', 'sk-waliapi-272d9aafb16145e2b25e8a7340a57ada', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM harness_model_setting WHERE profile_code = 'headless');

INSERT INTO harness_model_setting (profile_code, provider_code, model_code, base_url, api_key_ref, enabled, updated_at)
SELECT 'web', 'deepseek', 'gpt-5.5', 'http://127.0.0.1:8777/v1', 'sk-waliapi-272d9aafb16145e2b25e8a7340a57ada', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM harness_model_setting WHERE profile_code = 'web');

-- ── Tool catalog & per-profile bindings ──────────────────────
CREATE TABLE IF NOT EXISTS harness_tool_catalog (
    tool_code VARCHAR(128) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category VARCHAR(64) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS harness_tool_profile_binding (
    profile_code VARCHAR(64) NOT NULL,
    tool_code VARCHAR(128) NOT NULL,
    PRIMARY KEY (profile_code, tool_code),
    CONSTRAINT fk_harness_tool_binding_tool FOREIGN KEY (tool_code) REFERENCES harness_tool_catalog(tool_code)
);

-- ── Tool catalog seed data ───────────────────────────────────
INSERT INTO harness_tool_catalog (tool_code, display_name, description, category, requires_approval)
SELECT 'fs.read', 'Read File', 'Read file contents from the workspace.', 'fs', FALSE
WHERE NOT EXISTS (SELECT 1 FROM harness_tool_catalog WHERE tool_code = 'fs.read');

INSERT INTO harness_tool_catalog (tool_code, display_name, description, category, requires_approval)
SELECT 'fs.write', 'Write File', 'Write or overwrite file contents in the workspace.', 'fs', TRUE
WHERE NOT EXISTS (SELECT 1 FROM harness_tool_catalog WHERE tool_code = 'fs.write');

INSERT INTO harness_tool_catalog (tool_code, display_name, description, category, requires_approval)
SELECT 'shell.exec', 'Execute Shell', 'Run a shell command in the sandbox.', 'shell', TRUE
WHERE NOT EXISTS (SELECT 1 FROM harness_tool_catalog WHERE tool_code = 'shell.exec');

INSERT INTO harness_tool_catalog (tool_code, display_name, description, category, requires_approval)
SELECT 'web.search', 'Web Search', 'Search the web for information.', 'web', FALSE
WHERE NOT EXISTS (SELECT 1 FROM harness_tool_catalog WHERE tool_code = 'web.search');

INSERT INTO harness_tool_catalog (tool_code, display_name, description, category, requires_approval)
SELECT 'mcp.call', 'MCP Tool', 'Invoke an MCP server tool.', 'mcp', TRUE
WHERE NOT EXISTS (SELECT 1 FROM harness_tool_catalog WHERE tool_code = 'mcp.call');

-- Default: enable read-only tools for headless and web profiles
INSERT IGNORE INTO harness_tool_profile_binding (profile_code, tool_code) VALUES ('headless', 'fs.read');
INSERT IGNORE INTO harness_tool_profile_binding (profile_code, tool_code) VALUES ('headless', 'web.search');
INSERT IGNORE INTO harness_tool_profile_binding (profile_code, tool_code) VALUES ('web', 'fs.read');
INSERT IGNORE INTO harness_tool_profile_binding (profile_code, tool_code) VALUES ('web', 'web.search');
