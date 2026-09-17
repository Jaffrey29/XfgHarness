CREATE TABLE IF NOT EXISTS harness_session (
    session_id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(128) NOT NULL,
    profile_code VARCHAR(64) NOT NULL,
    execution_mode VARCHAR(32) NOT NULL,
    resolved_model VARCHAR(128) NOT NULL,
    channel_code VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS harness_task (
    task_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    prompt TEXT NOT NULL,
    enabled_tools_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS harness_session_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    KEY idx_harness_session_event_session (session_id)
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
    checked_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS harness_plugin_config (
    plugin_id VARCHAR(128) NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT NULL,
    config_layer VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (plugin_id, config_key, config_layer)
);

CREATE TABLE IF NOT EXISTS harness_model_setting (
    channel_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    base_url VARCHAR(255) NULL,
    api_key_ref VARCHAR(128) NULL,
    protocol VARCHAR(32) NOT NULL DEFAULT 'openai',
    enabled BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (channel_code)
);

-- 开发阶段模型渠道表以 channel_code 为主键；channel_code 与 harness_session.channel_code
-- 均由 SchemaMigrationRunner 在启动时幂等补齐。

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
    payload_json LONGTEXT NULL,
    surface_op VARCHAR(16) NULL,
    source_event_seqs VARCHAR(512) NULL,
    UNIQUE (session_id, seq),
    KEY idx_harness_session_event_log_session (session_id)
);

-- 模型配置的权威来源是本表；默认模型种子由 ModelSyncRunner 按 application.yml 初始化，
-- 不在 schema 中重复写入可能过期的 base_url、api_key 或模型编码。

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
    PRIMARY KEY (profile_code, tool_code)
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

-- ── 数字人协作 ─────────────────────────────────
-- 数字人身份（展示与策略，与连接解耦）
CREATE TABLE IF NOT EXISTS digital_human (
    id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    avatar_ref VARCHAR(255) NULL,
    purpose VARCHAR(1024) NOT NULL,
    role_tags VARCHAR(512) NULL,
    theme_color VARCHAR(16) NULL,
    approval_policy VARCHAR(32) NOT NULL DEFAULT 'WRITE_REQUIRES_APPROVAL',
    concurrency_limit INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);

-- 服务端点（连接配置）
CREATE TABLE IF NOT EXISTS digital_human_endpoint (
    id VARCHAR(64) PRIMARY KEY,
    digital_human_id VARCHAR(64) NOT NULL,
    endpoint_type VARCHAR(24) NOT NULL,
    base_url VARCHAR(512) NULL,
    credential_ref VARCHAR(128) NULL,
    protocol_version VARCHAR(16) NULL,
    agent_card TEXT NULL,
    health_state VARCHAR(16) NOT NULL DEFAULT 'unknown',
    last_checked_at TIMESTAMP(3) NULL,
    latency_ms INT NULL,
    KEY idx_dh_endpoint_human (digital_human_id)
);

-- 协作房间
CREATE TABLE IF NOT EXISTS collaboration_room (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    objective TEXT NULL,
    orchestration_mode VARCHAR(24) NOT NULL DEFAULT 'MANUAL',
    run_state VARCHAR(16) NOT NULL DEFAULT 'idle',
    created_at TIMESTAMP(3) NOT NULL
);

-- 房间参与者
CREATE TABLE IF NOT EXISTS room_participant (
    id VARCHAR(64) PRIMARY KEY,
    room_id VARCHAR(64) NOT NULL,
    digital_human_id VARCHAR(64) NOT NULL,
    room_role VARCHAR(16) NOT NULL DEFAULT 'member',
    presence VARCHAR(24) NOT NULL DEFAULT 'idle',
    active_task_label VARCHAR(255) NULL,
    joined_at TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_room_participant (room_id, digital_human_id),
    KEY idx_participant_room (room_id)
);

-- 协作任务
CREATE TABLE IF NOT EXISTS collaboration_task (
    id VARCHAR(64) PRIMARY KEY,
    room_id VARCHAR(64) NOT NULL,
    parent_task_id VARCHAR(64) NULL,
    title VARCHAR(255) NOT NULL,
    instruction TEXT NOT NULL,
    assigned_to VARCHAR(64) NULL,
    requested_by VARCHAR(64) NOT NULL,
    state VARCHAR(24) NOT NULL DEFAULT 'READY',
    depends_on VARCHAR(512) NULL,
    input_artifacts VARCHAR(512) NULL,
    output_artifacts VARCHAR(512) NULL,
    remote_session_id VARCHAR(128) NULL,
    trace_id VARCHAR(64) NOT NULL,
    error_summary VARCHAR(1024) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    KEY idx_task_room_state (room_id, state)
);

-- 协作产物
CREATE TABLE IF NOT EXISTS collaboration_artifact (
    id VARCHAR(64) PRIMARY KEY,
    room_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NULL,
    producer_id VARCHAR(64) NULL,
    kind VARCHAR(24) NOT NULL,
    title VARCHAR(255) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    content TEXT NULL,
    trace_id VARCHAR(64) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    KEY idx_artifact_room (room_id)
);

-- 统一房间事件日志（一切展示与恢复的源头）
CREATE TABLE IF NOT EXISTS room_event (
    id VARCHAR(64) PRIMARY KEY,
    room_id VARCHAR(64) NOT NULL,
    seq BIGINT NOT NULL,
    task_id VARCHAR(64) NULL,
    participant_id VARCHAR(64) NULL,
    digital_human_id VARCHAR(64) NULL,
    event_type VARCHAR(48) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'room',
    payload TEXT NULL,
    trace_id VARCHAR(64) NULL,
    UNIQUE KEY uk_room_event_seq (room_id, seq),
    KEY idx_event_room_seq (room_id, seq),
    KEY idx_event_task (task_id)
);
