-- ============================================================
-- Auth: email verification, password reset, policy, OAuth
-- ============================================================

-- ------------------------------------------------------------
-- auth.email_verification_tokens: 이메일 인증 토큰
-- ------------------------------------------------------------
CREATE TABLE auth.email_verification_tokens (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email_address_id  uuid NOT NULL REFERENCES auth.user_email_addresses(id),
    token_hash        varchar(255) NOT NULL UNIQUE,
    expires_at        timestamptz NOT NULL,
    used_at           timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_email_verification_tokens_email_address_id
    ON auth.email_verification_tokens (email_address_id);
CREATE INDEX idx_auth_email_verification_tokens_expires_at
    ON auth.email_verification_tokens (expires_at);

-- ------------------------------------------------------------
-- auth.password_reset_tokens: 비밀번호 재설정 토큰
-- ------------------------------------------------------------
CREATE TABLE auth.password_reset_tokens (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES auth.users(id),
    token_hash  varchar(255) NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_password_reset_tokens_user_id
    ON auth.password_reset_tokens (user_id);
CREATE INDEX idx_auth_password_reset_tokens_expires_at
    ON auth.password_reset_tokens (expires_at);

-- ------------------------------------------------------------
-- auth.policy_documents: 약관 문서
-- ------------------------------------------------------------
CREATE TABLE auth.policy_documents (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_code   varchar(60) NOT NULL,
    version       varchar(30) NOT NULL,
    language_code varchar(12) NOT NULL DEFAULT 'ko',
    title         varchar(200) NOT NULL,
    content_url   text,
    content_hash  varchar(255),
    is_required   boolean NOT NULL DEFAULT true,
    published_at  timestamptz NOT NULL DEFAULT now(),
    retired_at    timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_auth_policy_documents_code_version_language
    ON auth.policy_documents (policy_code, version, language_code)
    WHERE retired_at IS NULL;
CREATE INDEX idx_auth_policy_documents_language_code
    ON auth.policy_documents (language_code);
CREATE INDEX idx_auth_policy_documents_is_required
    ON auth.policy_documents (is_required);

-- 정책 seed 데이터 (고정 UUID)
INSERT INTO auth.policy_documents (id, policy_code, version, language_code, title, is_required) VALUES
    ('00000000-0000-0000-0000-000000000001', 'TERMS_OF_SERVICE', '1.0', 'ko', '이용약관', true),
    ('00000000-0000-0000-0000-000000000002', 'PRIVACY_POLICY',  '1.0', 'ko', '개인정보처리방침', true);

-- ------------------------------------------------------------
-- auth.user_policy_acceptances: 약관 동의 기록
-- ------------------------------------------------------------
CREATE TABLE auth.user_policy_acceptances (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             uuid NOT NULL REFERENCES auth.users(id),
    policy_document_id  uuid NOT NULL REFERENCES auth.policy_documents(id),
    acceptance_method   varchar(40) NOT NULL DEFAULT 'EXPLICIT',
    accepted_at         timestamptz NOT NULL DEFAULT now(),
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_user_policy_acceptances_user_id
    ON auth.user_policy_acceptances (user_id);
CREATE INDEX idx_auth_user_policy_acceptances_policy_document_id
    ON auth.user_policy_acceptances (policy_document_id);

-- ------------------------------------------------------------
-- auth.user_oauth_identities: OAuth 제공자 계정 연결
-- ------------------------------------------------------------
CREATE TABLE auth.user_oauth_identities (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               uuid NOT NULL REFERENCES auth.users(id),
    provider_id           smallint NOT NULL REFERENCES auth.auth_providers(id),
    provider_subject      varchar(320) NOT NULL,
    provider_email        varchar(320),
    provider_display_name varchar(120),
    linked_at             timestamptz NOT NULL DEFAULT now(),
    last_login_at         timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_auth_user_oauth_identities_provider_subject
    ON auth.user_oauth_identities (provider_id, provider_subject);
CREATE INDEX idx_auth_user_oauth_identities_user_id
    ON auth.user_oauth_identities (user_id);
