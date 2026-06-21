-- ============================================================
-- V20: ai 스키마 (ai_chat_sessions, ai_chat_messages, ai_tool_calls)
-- 근거: .agent/contracts/schema.dbml (lines 1272-1342)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS ai;

-- ------------------------------------------------------------
-- ai.ai_chat_sessions
-- trip_id unique (한 trip 당 1 session)
-- ------------------------------------------------------------
CREATE TABLE ai.ai_chat_sessions (
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id             uuid        NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE,
    status              varchar(30) NOT NULL DEFAULT 'ACTIVE',
    summary             text,
    summary_updated_at  timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    expires_at          timestamptz,
    deleted_at          timestamptz
);

CREATE UNIQUE INDEX uq_ai_chat_sessions_trip_id ON ai.ai_chat_sessions (trip_id);
CREATE INDEX idx_ai_chat_sessions_status        ON ai.ai_chat_sessions (status);

-- ------------------------------------------------------------
-- ai.ai_chat_messages
-- ------------------------------------------------------------
CREATE TABLE ai.ai_chat_messages (
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          uuid        NOT NULL REFERENCES ai.ai_chat_sessions(id) ON DELETE CASCADE,
    requester_user_id   uuid        REFERENCES auth.users(id),
    role                varchar(30) NOT NULL,
    content             text        NOT NULL,
    tool_call_id        uuid,
    metadata            jsonb,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_chat_messages_session_id             ON ai.ai_chat_messages (session_id);
CREATE INDEX idx_ai_chat_messages_requester_user_id      ON ai.ai_chat_messages (requester_user_id);
CREATE INDEX idx_ai_chat_messages_role                   ON ai.ai_chat_messages (role);
CREATE INDEX idx_ai_chat_messages_created_at             ON ai.ai_chat_messages (created_at);
CREATE INDEX idx_ai_chat_messages_session_created_id     ON ai.ai_chat_messages (session_id, created_at, id);
CREATE INDEX idx_ai_chat_messages_tool_call_id           ON ai.ai_chat_messages (tool_call_id);

-- ------------------------------------------------------------
-- ai.ai_tool_calls
-- ------------------------------------------------------------
CREATE TABLE ai.ai_tool_calls (
    id                      uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id              uuid        NOT NULL REFERENCES ai.ai_chat_sessions(id) ON DELETE CASCADE,
    trip_id                 uuid        NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE,
    request_message_id      uuid        REFERENCES ai.ai_chat_messages(id),
    result_message_id       uuid        REFERENCES ai.ai_chat_messages(id),
    requested_by_user_id    uuid        NOT NULL REFERENCES auth.users(id),
    websocket_session_id    varchar(120),
    tool_name               varchar(120) NOT NULL,
    execution_policy        varchar(40)  NOT NULL,
    arguments               jsonb       NOT NULL,
    result                  jsonb,
    status                  varchar(40)  NOT NULL DEFAULT 'REQUESTED',
    version_before          bigint,
    version_after           bigint,
    undo_redo_available     boolean      NOT NULL DEFAULT false,
    undo_command            jsonb,
    redo_command            jsonb,
    error_code              varchar(120),
    error_message           text,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    completed_at            timestamptz
);

CREATE INDEX idx_ai_tool_calls_session_id            ON ai.ai_tool_calls (session_id);
CREATE INDEX idx_ai_tool_calls_trip_id               ON ai.ai_tool_calls (trip_id);
CREATE INDEX idx_ai_tool_calls_requested_by_user_id  ON ai.ai_tool_calls (requested_by_user_id);
CREATE INDEX idx_ai_tool_calls_tool_name             ON ai.ai_tool_calls (tool_name);
CREATE INDEX idx_ai_tool_calls_status                ON ai.ai_tool_calls (status);
CREATE INDEX idx_ai_tool_calls_created_at            ON ai.ai_tool_calls (created_at);
