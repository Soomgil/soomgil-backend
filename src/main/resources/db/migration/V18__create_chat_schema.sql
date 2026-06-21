-- ============================================================
-- V18: chat 스키마 (trip_chat_messages)
-- 근거: .agent/contracts/schema.dbml (lines 970-988)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS chat;

CREATE TABLE chat.trip_chat_messages (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id           uuid        NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE,
    sender_user_id    uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content           text        NOT NULL,
    deleted_by_user_id uuid       REFERENCES auth.users(id),
    deleted_at        timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_trip_chat_messages_trip_id            ON chat.trip_chat_messages (trip_id);
CREATE INDEX idx_chat_trip_chat_messages_sender_user_id     ON chat.trip_chat_messages (sender_user_id);
CREATE INDEX idx_chat_trip_chat_messages_created_at         ON chat.trip_chat_messages (created_at);
CREATE INDEX idx_chat_trip_chat_messages_trip_created_id    ON chat.trip_chat_messages (trip_id, created_at, id);
CREATE INDEX idx_chat_trip_chat_messages_deleted_at         ON chat.trip_chat_messages (deleted_at);
