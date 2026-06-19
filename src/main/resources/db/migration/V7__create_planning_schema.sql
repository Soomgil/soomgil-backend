-- ============================================================
-- Planning: trip 단위 note, checklist, checklist item, member status
-- ============================================================

CREATE SCHEMA IF NOT EXISTS planning;

-- ------------------------------------------------------------
-- planning.notes: trip/scope/day 단위 note. 한 (trip, scope, day)에 1건.
-- ------------------------------------------------------------
CREATE TABLE planning.notes (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id          uuid NOT NULL,
    scope_type       varchar(10) NOT NULL,
    itinerary_day_id uuid,
    content          text NOT NULL,
    version          bigint NOT NULL DEFAULT 1,
    deleted_at       timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_planning_notes_trip_id
    ON planning.notes (trip_id);
CREATE INDEX idx_planning_notes_scope
    ON planning.notes (scope_type);
-- (trip_id, scope_type, itinerary_day_id) unique. PostgreSQL은 NULL을 distinct하게 보므로
-- itinerary_day_id NULL/NOT NULL 두 partial index로 분할.
CREATE UNIQUE INDEX uq_planning_notes_trip_scope_day
    ON planning.notes (trip_id, scope_type, itinerary_day_id)
    WHERE itinerary_day_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_planning_notes_trip_scope_nullday
    ON planning.notes (trip_id, scope_type)
    WHERE itinerary_day_id IS NULL AND deleted_at IS NULL;

-- ------------------------------------------------------------
-- planning.checklists: trip/scope/day 단위 checklist. 한 (trip, scope, day)에 1건.
-- title은 표현용 필드(nullable)이며 unique 대상이 아니다.
-- ------------------------------------------------------------
CREATE TABLE planning.checklists (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id          uuid NOT NULL,
    scope_type       varchar(10) NOT NULL,
    itinerary_day_id uuid,
    title            varchar(200),
    version          bigint NOT NULL DEFAULT 1,
    deleted_at       timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_planning_checklists_trip_id
    ON planning.checklists (trip_id);
CREATE INDEX idx_planning_checklists_scope
    ON planning.checklists (scope_type);
CREATE UNIQUE INDEX uq_planning_checklists_trip_scope_day
    ON planning.checklists (trip_id, scope_type, itinerary_day_id)
    WHERE itinerary_day_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_planning_checklists_trip_scope_nullday
    ON planning.checklists (trip_id, scope_type)
    WHERE itinerary_day_id IS NULL AND deleted_at IS NULL;

-- ------------------------------------------------------------
-- planning.checklist_items: checklist에 속한 item. sort_order 순서 보존.
-- soft delete 시 reorder에서 제외된다.
-- ------------------------------------------------------------
CREATE TABLE planning.checklist_items (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    checklist_id  uuid NOT NULL REFERENCES planning.checklists(id) ON DELETE CASCADE,
    sort_order    integer NOT NULL DEFAULT 0,
    content       varchar(500) NOT NULL,
    version       bigint NOT NULL DEFAULT 1,
    deleted_at    timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_planning_checklist_items_checklist_id
    ON planning.checklist_items (checklist_id);
CREATE INDEX idx_planning_checklist_items_sort_order
    ON planning.checklist_items (checklist_id, sort_order);

-- ------------------------------------------------------------
-- planning.checklist_item_member_status: 각 멤버의 item 완료 상태.
-- (item_id, user_id) 복합 PK로 중복 방지. 첫 토글 시 INSERT.
-- ------------------------------------------------------------
CREATE TABLE planning.checklist_item_member_status (
    item_id       uuid NOT NULL REFERENCES planning.checklist_items(id) ON DELETE CASCADE,
    user_id       uuid NOT NULL REFERENCES auth.users(id),
    is_completed  boolean NOT NULL DEFAULT false,
    completed_at  timestamptz,
    version       bigint NOT NULL DEFAULT 1,
    updated_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (item_id, user_id)
);

CREATE INDEX idx_planning_item_member_status_user_id
    ON planning.checklist_item_member_status (user_id);
