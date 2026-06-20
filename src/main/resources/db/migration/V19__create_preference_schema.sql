-- ============================================================
-- V19: preference 스키마 (preference_tags, place_tag_enrichments,
--      place_tag_enrichment_tags, user_place_reactions,
--      user_swipe_events, user_preference_tag_weights,
--      user_saved_places)
-- 근거: .agent/contracts/schema.dbml (lines 994-1266)
--
-- 주의: tag_statistic_runs, tag_statistics, synthetic_personas,
--       synthetic_persona_tag_preferences, synthetic_swipe_events,
--       place_tag_enrichment_candidates 도 DBML에 정의되어 있으나
--       더미 데이터에서 사용하지 않으므로 본 마이그레이션에서는 제외.
--       (참조 무결성상 이들 없이도 동작)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS preference;

-- ------------------------------------------------------------
-- preference.preference_tags
-- ------------------------------------------------------------
CREATE TABLE preference.preference_tags (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                varchar(80)  NOT NULL UNIQUE,
    display_name        varchar(120) NOT NULL,
    group_code          varchar(80)  NOT NULL,
    tag_type            varchar(20)  NOT NULL DEFAULT 'TAG',
    parent_tag_id       uuid         REFERENCES preference.preference_tags(id),
    description         text,
    is_selectable       boolean      NOT NULL DEFAULT true,
    is_active           boolean      NOT NULL DEFAULT true,
    dictionary_version  varchar(80)  NOT NULL DEFAULT 'preference-tags-v1',
    sort_order          int,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_preference_preference_tags_group_code        ON preference.preference_tags (group_code);
CREATE INDEX idx_preference_preference_tags_tag_type          ON preference.preference_tags (tag_type);
CREATE INDEX idx_preference_preference_tags_parent_tag_id     ON preference.preference_tags (parent_tag_id);
CREATE INDEX idx_preference_preference_tags_is_selectable     ON preference.preference_tags (is_selectable);
CREATE INDEX idx_preference_preference_tags_is_active         ON preference.preference_tags (is_active);
CREATE INDEX idx_preference_preference_tags_dictionary_version ON preference.preference_tags (dictionary_version);

-- ------------------------------------------------------------
-- preference.place_tag_enrichments
-- ------------------------------------------------------------
CREATE TABLE preference.place_tag_enrichments (
    id                          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    provider                    varchar(40)  NOT NULL,
    external_place_id           varchar(120) NOT NULL,
    source_modified_at          timestamptz,
    source_hash                 varchar(128),
    status                      varchar(30)  NOT NULL DEFAULT 'PENDING',
    model_provider              varchar(80),
    model_name                  varchar(120),
    prompt_version              varchar(80),
    tag_dictionary_version      varchar(80),
    selection_policy_version    varchar(80),
    tag_statistic_run_id        uuid,
    candidate_count             int          NOT NULL DEFAULT 0,
    selected_count              int          NOT NULL DEFAULT 0,
    error_message               text,
    enriched_at                 timestamptz,
    created_at                  timestamptz  NOT NULL DEFAULT now(),
    updated_at                  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_preference_place_tag_enrichments_provider_external ON preference.place_tag_enrichments (provider, external_place_id);
CREATE INDEX idx_preference_place_tag_enrichments_source_hash       ON preference.place_tag_enrichments (source_hash);
CREATE INDEX idx_preference_place_tag_enrichments_status             ON preference.place_tag_enrichments (status);
CREATE INDEX idx_preference_place_tag_enrichments_tag_statistic_run_id ON preference.place_tag_enrichments (tag_statistic_run_id);
CREATE INDEX idx_preference_place_tag_enrichments_enriched_at        ON preference.place_tag_enrichments (enriched_at);

-- ------------------------------------------------------------
-- preference.place_tag_enrichment_tags
-- 복합 PK: (enrichment_id, tag_id)
-- ------------------------------------------------------------
CREATE TABLE preference.place_tag_enrichment_tags (
    enrichment_id                       uuid           NOT NULL REFERENCES preference.place_tag_enrichments(id) ON DELETE CASCADE,
    tag_id                              uuid           NOT NULL REFERENCES preference.preference_tags(id),
    confidence                          decimal(5,4)   NOT NULL,
    weight                              decimal(8,4)   NOT NULL,
    preference_discrimination_snapshot  decimal(8,6),
    selection_score                     decimal(8,6),
    rank_order                          int            NOT NULL,
    tag_statistic_run_id                uuid,
    rationale                           text,
    PRIMARY KEY (enrichment_id, tag_id)
);

CREATE INDEX idx_preference_petenrichment_tags_tag_id                 ON preference.place_tag_enrichment_tags (tag_id);
CREATE INDEX idx_preference_petenrichment_tags_tag_statistic_run_id   ON preference.place_tag_enrichment_tags (tag_statistic_run_id);
CREATE INDEX idx_preference_petenrichment_tags_rank_order             ON preference.place_tag_enrichment_tags (rank_order);

-- ------------------------------------------------------------
-- preference.user_place_reactions
-- unique: (user_id, provider, external_place_id)
-- ------------------------------------------------------------
CREATE TABLE preference.user_place_reactions (
    id                          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     uuid         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    provider                    varchar(40)  NOT NULL,
    external_place_id           varchar(120) NOT NULL,
    reaction                    varchar(20)  NOT NULL,
    reaction_count              int          NOT NULL DEFAULT 1,
    first_reacted_at            timestamptz  NOT NULL DEFAULT now(),
    last_reacted_at             timestamptz  NOT NULL DEFAULT now(),
    source_modified_at          timestamptz,
    place_tag_enrichment_id     uuid         REFERENCES preference.place_tag_enrichments(id)
);

CREATE UNIQUE INDEX uq_preference_user_place_reactions_user_provider_external
    ON preference.user_place_reactions (user_id, provider, external_place_id);
CREATE INDEX idx_preference_user_place_reactions_user_id        ON preference.user_place_reactions (user_id);
CREATE INDEX idx_preference_user_place_reactions_reaction       ON preference.user_place_reactions (reaction);
CREATE INDEX idx_preference_user_place_reactions_provider_external ON preference.user_place_reactions (provider, external_place_id);

-- ------------------------------------------------------------
-- preference.user_swipe_events
-- bigserial PK (이벤트 로그)
-- ------------------------------------------------------------
CREATE TABLE preference.user_swipe_events (
    id                          bigserial    PRIMARY KEY,
    user_id                     uuid         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    provider                    varchar(40)  NOT NULL,
    external_place_id           varchar(120) NOT NULL,
    reaction                    varchar(20)  NOT NULL,
    previous_reaction           varchar(20),
    feed_context                jsonb,
    source_modified_at          timestamptz,
    place_tag_enrichment_id     uuid         REFERENCES preference.place_tag_enrichments(id),
    occurred_at                 timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_preference_user_swipe_events_user_id             ON preference.user_swipe_events (user_id);
CREATE INDEX idx_preference_user_swipe_events_provider_external   ON preference.user_swipe_events (provider, external_place_id);
CREATE INDEX idx_preference_user_swipe_events_reaction            ON preference.user_swipe_events (reaction);
CREATE INDEX idx_preference_user_swipe_events_occurred_at         ON preference.user_swipe_events (occurred_at);

-- ------------------------------------------------------------
-- preference.user_preference_tag_weights
-- 복합 PK: (user_id, tag_id)
-- ------------------------------------------------------------
CREATE TABLE preference.user_preference_tag_weights (
    user_id              uuid           NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    tag_id               uuid           NOT NULL REFERENCES preference.preference_tags(id),
    raw_score            decimal(14,4)  NOT NULL DEFAULT 0,
    normalized_score     decimal(8,6)   NOT NULL DEFAULT 0,
    like_count           int            NOT NULL DEFAULT 0,
    super_like_count     int            NOT NULL DEFAULT 0,
    nope_count           int            NOT NULL DEFAULT 0,
    updated_at           timestamptz    NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, tag_id)
);

CREATE INDEX idx_preference_user_preference_tag_weights_tag_id            ON preference.user_preference_tag_weights (tag_id);
CREATE INDEX idx_preference_user_preference_tag_weights_normalized_score  ON preference.user_preference_tag_weights (normalized_score);

-- ------------------------------------------------------------
-- preference.user_saved_places
-- unique: (user_id, provider, external_place_id)
-- ------------------------------------------------------------
CREATE TABLE preference.user_saved_places (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             uuid         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    provider            varchar(40)  NOT NULL,
    external_place_id   varchar(120) NOT NULL,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    deleted_at          timestamptz
);

CREATE UNIQUE INDEX uq_preference_user_saved_places_user_provider_external
    ON preference.user_saved_places (user_id, provider, external_place_id);
CREATE INDEX idx_preference_user_saved_places_provider_external ON preference.user_saved_places (provider, external_place_id);
CREATE INDEX idx_preference_user_saved_places_deleted_at        ON preference.user_saved_places (deleted_at);
