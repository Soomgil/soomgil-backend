-- ============================================================
-- V17: social 스키마 (user_follows)
-- 근거: .agent/contracts/schema.dbml (lines 518-534)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS social;

CREATE TABLE social.user_follows (
    follower_user_id  uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    following_user_id uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status            varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    deleted_at        timestamptz,
    CONSTRAINT user_follows_no_self_check CHECK (follower_user_id <> following_user_id),
    CONSTRAINT user_follows_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'DELETED')),
    PRIMARY KEY (follower_user_id, following_user_id)
);

CREATE INDEX idx_social_user_follows_following_user_id     ON social.user_follows (following_user_id);
CREATE INDEX idx_social_user_follows_following_status      ON social.user_follows (following_user_id, status);
CREATE INDEX idx_social_user_follows_status                ON social.user_follows (status);
