-- ============================================================
-- Identity and Access: auth schema
-- ============================================================

CREATE SCHEMA IF NOT EXISTS auth;

-- ------------------------------------------------------------
-- auth.users: account lifecycle anchor
-- ------------------------------------------------------------
CREATE TABLE auth.users (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    status               varchar(30) NOT NULL DEFAULT 'ACTIVE',
    status_reason        varchar(255),
    status_changed_at    timestamptz NOT NULL DEFAULT now(),
    last_login_at        timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deletion_requested_at timestamptz,
    deletion_scheduled_at timestamptz,
    deleted_at           timestamptz
);

CREATE INDEX idx_auth_users_status ON auth.users (status);
CREATE INDEX idx_auth_users_status_changed_at ON auth.users (status_changed_at);
CREATE INDEX idx_auth_users_created_at ON auth.users (created_at);
CREATE INDEX idx_auth_users_deletion_scheduled_at ON auth.users (deletion_scheduled_at);
CREATE INDEX idx_auth_users_deleted_at ON auth.users (deleted_at);

-- ------------------------------------------------------------
-- auth.user_email_addresses: login email history
-- ------------------------------------------------------------
CREATE TABLE auth.user_email_addresses (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     uuid NOT NULL REFERENCES auth.users(id),
    email                       varchar(320) NOT NULL,
    normalized_email            varchar(320) NOT NULL,
    is_primary                  boolean NOT NULL DEFAULT false,
    verified_at                 timestamptz,
    verification_last_sent_at   timestamptz,
    removed_at                  timestamptz,
    removed_reason              varchar(120),
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_auth_user_email_addresses_active_normalized_email
    ON auth.user_email_addresses (normalized_email) WHERE removed_at IS NULL;
CREATE UNIQUE INDEX uq_auth_user_email_addresses_one_active_primary
    ON auth.user_email_addresses (user_id) WHERE is_primary = true AND removed_at IS NULL;
CREATE INDEX idx_auth_user_email_addresses_user_id ON auth.user_email_addresses (user_id);
CREATE INDEX idx_auth_user_email_addresses_normalized_email ON auth.user_email_addresses (normalized_email);
CREATE INDEX idx_auth_user_email_addresses_verified_at ON auth.user_email_addresses (verified_at);
CREATE INDEX idx_auth_user_email_addresses_removed_at ON auth.user_email_addresses (removed_at);

-- ------------------------------------------------------------
-- auth.user_profiles: display data
-- ------------------------------------------------------------
CREATE TABLE auth.user_profiles (
    user_id               uuid PRIMARY KEY REFERENCES auth.users(id),
    display_name          varchar(80) NOT NULL,
    profile_image_url     text,
    profile_media_file_id uuid,
    bio                   varchar(500),
    profile_visibility    varchar(20) NOT NULL DEFAULT 'PUBLIC',
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_user_profiles_display_name ON auth.user_profiles (display_name);
CREATE INDEX idx_auth_user_profiles_profile_visibility ON auth.user_profiles (profile_visibility);

-- ------------------------------------------------------------
-- auth.user_settings: per-user preferences
-- ------------------------------------------------------------
CREATE TABLE auth.user_settings (
    user_id                    uuid PRIMARY KEY REFERENCES auth.users(id),
    display_language           varchar(12) NOT NULL DEFAULT 'ko',
    timezone                   varchar(50) NOT NULL DEFAULT 'Asia/Seoul',
    marketing_email_opt_in     boolean NOT NULL DEFAULT false,
    marketing_email_opted_in_at  timestamptz,
    marketing_email_opted_out_at timestamptz,
    trip_invite_email_opt_in   boolean NOT NULL DEFAULT true,
    created_at                 timestamptz NOT NULL DEFAULT now(),
    updated_at                 timestamptz NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- auth.auth_providers: LOCAL, KAKAO, GOOGLE
-- ------------------------------------------------------------
CREATE TABLE auth.auth_providers (
    id           smallserial PRIMARY KEY,
    code         varchar(40) NOT NULL UNIQUE,
    display_name varchar(80) NOT NULL,
    auth_type    varchar(30) NOT NULL DEFAULT 'OAUTH2',
    issuer       text,
    is_active    boolean NOT NULL DEFAULT true,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);

INSERT INTO auth.auth_providers (code, display_name, auth_type) VALUES
    ('LOCAL', 'Local', 'LOCAL'),
    ('KAKAO', 'Kakao', 'OAUTH2'),
    ('GOOGLE', 'Google', 'OAUTH2');

-- ------------------------------------------------------------
-- auth.user_password_credentials: bcrypt password hash
-- ------------------------------------------------------------
CREATE TABLE auth.user_password_credentials (
    user_id              uuid PRIMARY KEY REFERENCES auth.users(id),
    password_hash        varchar(255) NOT NULL,
    password_algorithm   varchar(40) NOT NULL DEFAULT 'bcrypt',
    password_hash_params jsonb,
    password_changed_at  timestamptz NOT NULL DEFAULT now(),
    failed_login_count   int NOT NULL DEFAULT 0,
    locked_until         timestamptz,
    last_failed_login_at timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_user_password_credentials_locked_until ON auth.user_password_credentials (locked_until);

-- ------------------------------------------------------------
-- auth.user_sessions: refresh token store
-- ------------------------------------------------------------
CREATE TABLE auth.user_sessions (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  uuid NOT NULL REFERENCES auth.users(id),
    refresh_token_hash       varchar(255) NOT NULL UNIQUE,
    refresh_token_family_id  uuid NOT NULL DEFAULT gen_random_uuid(),
    refresh_token_version    int NOT NULL DEFAULT 1,
    device_name              varchar(120),
    device_os                varchar(120),
    ip_address_hash          varchar(255),
    user_agent_hash          varchar(255),
    last_used_at             timestamptz,
    last_refreshed_at        timestamptz,
    expires_at               timestamptz NOT NULL,
    revoked_at               timestamptz,
    revoked_by_user_id       uuid,
    revocation_reason        varchar(120),
    created_at               timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_user_sessions_user_id ON auth.user_sessions (user_id);
CREATE INDEX idx_auth_user_sessions_family_id ON auth.user_sessions (refresh_token_family_id);
CREATE INDEX idx_auth_user_sessions_expires_at ON auth.user_sessions (expires_at);
CREATE INDEX idx_auth_user_sessions_revoked_at ON auth.user_sessions (revoked_at);

-- ------------------------------------------------------------
-- auth.user_security_events: login audit log
-- ------------------------------------------------------------
CREATE TABLE auth.user_security_events (
    id                    bigserial PRIMARY KEY,
    user_id               uuid REFERENCES auth.users(id),
    actor_user_id         uuid,
    provider_id           smallint REFERENCES auth.auth_providers(id),
    normalized_identifier varchar(320),
    event_type            varchar(80) NOT NULL,
    success               boolean NOT NULL DEFAULT false,
    failure_reason        varchar(120),
    ip_address_hash       varchar(255),
    user_agent_hash       varchar(255),
    metadata              jsonb,
    created_at            timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_user_security_events_user_id ON auth.user_security_events (user_id);
CREATE INDEX idx_auth_user_security_events_event_type ON auth.user_security_events (event_type);
CREATE INDEX idx_auth_user_security_events_created_at ON auth.user_security_events (created_at);
