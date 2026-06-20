-- ============================================================
-- Phase 3: 신고(Reports) + 모더레이션(Moderation)
-- ============================================================

-- ------------------------------------------------------------
-- auth.user_roles: 모더레이터/관리자 역할 부여 이력.
-- DBML 기준: (user_id, role_id) PK + granted/revoked 추적 컬럼.
-- JWT 발급 시 활성(role_id가 revoked_at IS NULL) 역할만 claim으로 반영된다.
-- ------------------------------------------------------------
CREATE TABLE auth.user_roles (
    user_id           uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id           smallint    NOT NULL REFERENCES auth.roles(id),
    granted_by_user_id uuid       REFERENCES auth.users(id),
    granted_at        timestamptz NOT NULL DEFAULT now(),
    revoked_at        timestamptz,
    revoked_by_user_id uuid       REFERENCES auth.users(id),
    revocation_reason varchar(120),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_auth_user_roles_role_id
    ON auth.user_roles (role_id);
CREATE INDEX idx_auth_user_roles_granted_by_user_id
    ON auth.user_roles (granted_by_user_id);
CREATE INDEX idx_auth_user_roles_revoked_by_user_id
    ON auth.user_roles (revoked_by_user_id);
CREATE INDEX idx_auth_user_roles_revoked_at
    ON auth.user_roles (revoked_at);

-- ------------------------------------------------------------
-- community.report_reasons: 신고 사유 마스터. is_active=false로 비활성화 가능.
-- ------------------------------------------------------------
CREATE TABLE community.report_reasons (
    code         varchar(40) PRIMARY KEY,
    display_name varchar(100) NOT NULL,
    is_active    boolean NOT NULL DEFAULT true,
    sort_order   integer NOT NULL DEFAULT 0
);

INSERT INTO community.report_reasons (code, display_name, is_active, sort_order) VALUES
    ('SPAM',                '스팸 · 광고',         true, 1),
    ('INAPPROPRIATE',       '부적절한 내용',        true, 2),
    ('HARASSMENT_OR_HATE',  '괴롭힘 · 혐오 표현',   true, 3),
    ('RIGHTS_VIOLATION',    '저작권 · 초상권 침해', true, 4),
    ('OTHER',               '기타',                true, 5);

-- ------------------------------------------------------------
-- community.content_reports: 사용자 신고 건.
-- ------------------------------------------------------------
CREATE TABLE community.content_reports (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_user_id uuid NOT NULL REFERENCES auth.users(id),
    target_type      varchar(20) NOT NULL,
    target_id        uuid NOT NULL,
    reason_code      varchar(40) NOT NULL REFERENCES community.report_reasons(code),
    detail           text,
    status           varchar(20) NOT NULL DEFAULT 'OPEN',
    resolution_note  text,
    resolved_by      uuid REFERENCES auth.users(id),
    resolved_at      timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_content_reports_status
    ON community.content_reports (status, created_at DESC);
CREATE INDEX idx_content_reports_target
    ON community.content_reports (target_type, target_id);
CREATE UNIQUE INDEX uq_content_reports_reporter_target
    ON community.content_reports (reporter_user_id, target_type, target_id)
    WHERE status IN ('OPEN', 'REVIEWING');

-- ------------------------------------------------------------
-- community.moderation_actions: 모더레이터 조치 이력.
-- ------------------------------------------------------------
CREATE TABLE community.moderation_actions (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    moderator_user_id uuid NOT NULL REFERENCES auth.users(id),
    target_type       varchar(20) NOT NULL,
    target_id         uuid NOT NULL,
    action            varchar(20) NOT NULL,
    moderation_status varchar(20),
    moderation_reason varchar(255),
    report_id         uuid REFERENCES community.content_reports(id) ON DELETE SET NULL,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_moderation_actions_target
    ON community.moderation_actions (target_type, target_id);
CREATE INDEX idx_moderation_actions_moderator
    ON community.moderation_actions (moderator_user_id);
CREATE INDEX idx_moderation_actions_report
    ON community.moderation_actions (report_id)
    WHERE report_id IS NOT NULL;
