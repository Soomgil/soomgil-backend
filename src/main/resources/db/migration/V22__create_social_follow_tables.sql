CREATE SCHEMA IF NOT EXISTS social;

CREATE TABLE social.user_follows (
  follower_user_id uuid NOT NULL,
  following_user_id uuid NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  PRIMARY KEY (follower_user_id, following_user_id),
  CONSTRAINT user_follows_no_self_check CHECK (follower_user_id != following_user_id),
  CONSTRAINT user_follows_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'DELETED'))
);

CREATE INDEX user_follows_following_status_idx
  ON social.user_follows (following_user_id, status);
CREATE INDEX user_follows_status_idx ON social.user_follows (status);
