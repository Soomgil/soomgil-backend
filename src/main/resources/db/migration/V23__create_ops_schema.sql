-- ============================================================
-- V23: ops 스키마 (audit_logs)
-- 근거: .agent/contracts/schema.dbml (lines 1642-1661)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS ops;

CREATE TABLE ops.audit_logs (
    id             bigserial   PRIMARY KEY,
    actor_user_id  uuid,
    action         varchar(120) NOT NULL,
    target_type    varchar(80),
    target_id      uuid,
    ip_address     varchar(64),
    user_agent     text,
    metadata       jsonb,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ops_audit_logs_actor_user_id     ON ops.audit_logs (actor_user_id);
CREATE INDEX idx_ops_audit_logs_target_type_id    ON ops.audit_logs (target_type, target_id);
CREATE INDEX idx_ops_audit_logs_action            ON ops.audit_logs (action);
CREATE INDEX idx_ops_audit_logs_created_at        ON ops.audit_logs (created_at);
