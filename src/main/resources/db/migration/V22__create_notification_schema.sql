-- ============================================================
-- V22: notification 스키마 (notifications)
-- 근거: .agent/contracts/schema.dbml (lines 688-705)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.notifications (
    id                 uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id  uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    actor_user_id      uuid        REFERENCES auth.users(id),
    trip_id            uuid        REFERENCES trip.trips(id) ON DELETE CASCADE,
    type               varchar(60) NOT NULL,
    title              varchar(160) NOT NULL,
    body               text,
    payload            jsonb,
    read_at            timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_notifications_recipient_read     ON notification.notifications (recipient_user_id, read_at);
CREATE INDEX idx_notification_notifications_recipient_created  ON notification.notifications (recipient_user_id, created_at);
CREATE INDEX idx_notification_notifications_trip_id            ON notification.notifications (trip_id);
