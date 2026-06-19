-- ============================================================
-- V3: schema.dbml 일치화 (rename + 누락 컬럼 추가)
-- 근거: .agent/contracts/schema.dbml
-- ============================================================

-- ------------------------------------------------------------
-- 1. 테이블명 rename (DBML 기준)
-- ------------------------------------------------------------
ALTER TABLE auth.email_verification_tokens  RENAME TO user_email_verification_tokens;
ALTER TABLE auth.password_reset_tokens      RENAME TO user_password_reset_tokens;
ALTER TABLE auth.user_oauth_identities      RENAME TO user_auth_identities;

-- 인덱스 rename (PostgreSQL은 테이블 rename 시 인덱스명을 자동 변경하지 않음)
-- 주의: ALTER INDEX는 search_path에서만 찾으므로 schema-qualified 이름 사용.
-- 새 이름(new_name)은 schema를 포함할 수 없다 (부모 테이블과 같은 schema에 남음).
ALTER INDEX auth.idx_auth_email_verification_tokens_email_address_id
    RENAME TO idx_auth_user_email_verification_tokens_user_email_address_id;
ALTER INDEX auth.idx_auth_email_verification_tokens_expires_at
    RENAME TO idx_auth_user_email_verification_tokens_expires_at;
ALTER INDEX auth.idx_auth_password_reset_tokens_user_id
    RENAME TO idx_auth_user_password_reset_tokens_user_id;
ALTER INDEX auth.idx_auth_password_reset_tokens_expires_at
    RENAME TO idx_auth_user_password_reset_tokens_expires_at;
ALTER INDEX auth.uq_auth_user_oauth_identities_provider_subject
    RENAME TO uq_auth_user_auth_identities_provider_subject;
ALTER INDEX auth.idx_auth_user_oauth_identities_user_id
    RENAME TO idx_auth_user_auth_identities_user_id;

-- ------------------------------------------------------------
-- 2. 컬럼명 rename
-- ------------------------------------------------------------
ALTER TABLE auth.user_email_verification_tokens
    RENAME COLUMN email_address_id TO user_email_address_id;

-- ------------------------------------------------------------
-- 3. DBML에 정의됐지만 V2에 없는 컬럼 추가
-- ------------------------------------------------------------

-- auth.user_email_verification_tokens.requested_by_ip_hash
ALTER TABLE auth.user_email_verification_tokens
    ADD COLUMN requested_by_ip_hash varchar(255);

-- auth.user_password_reset_tokens.requested_by_ip_hash
ALTER TABLE auth.user_password_reset_tokens
    ADD COLUMN requested_by_ip_hash varchar(255);

-- auth.user_auth_identities: provider_email_verified, provider_profile
ALTER TABLE auth.user_auth_identities
    ADD COLUMN provider_email_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN provider_profile jsonb;
CREATE INDEX idx_auth_user_auth_identities_provider_email
    ON auth.user_auth_identities (provider_id, provider_email);

-- auth.user_policy_acceptances: ip_address_hash, user_agent_hash
ALTER TABLE auth.user_policy_acceptances
    ADD COLUMN ip_address_hash varchar(255),
    ADD COLUMN user_agent_hash varchar(255);

-- ------------------------------------------------------------
-- 4. policy_documents 컬럼 길이를 DBML 기준으로 확장
-- DBML: policy_code varchar(80), version varchar(40), title varchar(160), content_hash varchar(128)
-- ------------------------------------------------------------
ALTER TABLE auth.policy_documents ALTER COLUMN policy_code   TYPE varchar(80);
ALTER TABLE auth.policy_documents ALTER COLUMN version       TYPE varchar(40);
ALTER TABLE auth.policy_documents ALTER COLUMN title         TYPE varchar(160);
ALTER TABLE auth.policy_documents ALTER COLUMN content_hash  TYPE varchar(128);
