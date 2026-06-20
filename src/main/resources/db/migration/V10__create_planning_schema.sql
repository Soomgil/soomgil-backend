-- ============================================================
-- Planning: trip_notes / checklists / checklist_items / checklist_item_member_statuses
-- 근거: .agent/contracts/schema.dbml (lines 887-964)
-- 주의: DBML에 version 컬럼 없음. optimistic locking은 상위 itinerary_version
-- 단위로만 관리되며 planning 리소스 개별 version은 두지 않는다.
-- ============================================================

CREATE SCHEMA IF NOT EXISTS planning;

-- ------------------------------------------------------------
-- planning.trip_notes
-- (trip_id, scope_type, itinerary_day_id) 조합으로 unique.
-- itinerary_day_id가 NULL인 TRIP scope은 별도 partial unique index로 분리
-- (PostgreSQL은 NULL을 distinct하게 취급).
-- ------------------------------------------------------------
CREATE TABLE planning.trip_notes (
    id                   uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id              uuid        NOT NULL,
    scope_type           varchar(20) NOT NULL,
    itinerary_day_id     uuid,
    content              text        NOT NULL DEFAULT '',
    created_by_user_id   uuid,
    updated_by_user_id   uuid,
    deleted_by_user_id   uuid,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz
);

CREATE INDEX idx_planning_trip_notes_trip_id
    ON planning.trip_notes (trip_id);
CREATE INDEX idx_planning_trip_notes_itinerary_day_id
    ON planning.trip_notes (itinerary_day_id);
CREATE UNIQUE INDEX uq_planning_trip_notes_trip_scope_day
    ON planning.trip_notes (trip_id, scope_type, itinerary_day_id)
    WHERE itinerary_day_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_planning_trip_notes_trip_scope_nullday
    ON planning.trip_notes (trip_id, scope_type)
    WHERE itinerary_day_id IS NULL AND deleted_at IS NULL;

-- ------------------------------------------------------------
-- planning.checklists
-- ------------------------------------------------------------
CREATE TABLE planning.checklists (
    id                   uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id              uuid        NOT NULL,
    scope_type           varchar(20) NOT NULL,
    itinerary_day_id     uuid,
    title                varchar(120),
    created_by_user_id   uuid,
    updated_by_user_id   uuid,
    deleted_by_user_id   uuid,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz
);

CREATE INDEX idx_planning_checklists_trip_id
    ON planning.checklists (trip_id);
CREATE INDEX idx_planning_checklists_itinerary_day_id
    ON planning.checklists (itinerary_day_id);
CREATE UNIQUE INDEX uq_planning_checklists_trip_scope_day
    ON planning.checklists (trip_id, scope_type, itinerary_day_id)
    WHERE itinerary_day_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_planning_checklists_trip_scope_nullday
    ON planning.checklists (trip_id, scope_type)
    WHERE itinerary_day_id IS NULL AND deleted_at IS NULL;

-- ------------------------------------------------------------
-- planning.checklist_items
-- ------------------------------------------------------------
CREATE TABLE planning.checklist_items (
    id                   uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    checklist_id         uuid        NOT NULL REFERENCES planning.checklists(id) ON DELETE CASCADE,
    sort_order           int         NOT NULL,
    content              text        NOT NULL,
    created_by_user_id   uuid,
    updated_by_user_id   uuid,
    deleted_by_user_id   uuid,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz
);

CREATE INDEX idx_planning_checklist_items_checklist_id
    ON planning.checklist_items (checklist_id);
CREATE INDEX idx_planning_checklist_items_checklist_sort
    ON planning.checklist_items (checklist_id, sort_order);
CREATE INDEX idx_planning_checklist_items_deleted_at
    ON planning.checklist_items (deleted_at);

-- ------------------------------------------------------------
-- planning.checklist_item_member_statuses
-- 복합 PK: (checklist_item_id, user_id)
-- ------------------------------------------------------------
CREATE TABLE planning.checklist_item_member_statuses (
    checklist_item_id    uuid        NOT NULL REFERENCES planning.checklist_items(id) ON DELETE CASCADE,
    user_id              uuid        NOT NULL,
    is_completed         boolean     NOT NULL DEFAULT false,
    completed_at         timestamptz,
    updated_by_user_id   uuid,
    updated_at           timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (checklist_item_id, user_id)
);

CREATE INDEX idx_planning_checklist_item_member_statuses_user_id
    ON planning.checklist_item_member_statuses (user_id);
CREATE INDEX idx_planning_checklist_item_member_statuses_is_completed
    ON planning.checklist_item_member_statuses (is_completed);
CREATE INDEX idx_planning_checklist_item_member_statuses_updated_at
    ON planning.checklist_item_member_statuses (updated_at);
