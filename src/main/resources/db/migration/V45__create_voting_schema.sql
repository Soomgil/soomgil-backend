-- ============================================================
-- Voting: 여행 방 스티커 투표
-- ============================================================
-- 방장이 투표를 시작하면 그 시점의 활성 참여자와 추천 후보를 세션에 snapshot으로 고정한다.
-- 투표 도중 추천 결과가 바뀌어도 후보는 변하지 않는다.

CREATE SCHEMA IF NOT EXISTS voting;

-- ------------------------------------------------------------
-- voting.vote_sessions: 여행방당 하나의 투표 세션.
-- DRAFT는 향후 확장을 위해 값으로만 열어 두고 V1 API로는 생성하지 않는다.
-- ------------------------------------------------------------
CREATE TABLE voting.vote_sessions (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id               uuid NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE,
    status                varchar(20) NOT NULL DEFAULT 'OPEN',
    created_by_user_id    uuid NOT NULL REFERENCES auth.users(id),
    sticker_allowance     integer NOT NULL,
    selection_count       integer NOT NULL,
    candidate_count       integer NOT NULL,
    opened_at             timestamptz,
    completed_at          timestamptz,
    completion_reason     varchar(30),
    completed_by_user_id  uuid REFERENCES auth.users(id),
    result_applied_at     timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT vote_sessions_status_check
        CHECK (status IN ('DRAFT', 'OPEN', 'COMPLETED')),
    CONSTRAINT vote_sessions_completion_reason_check
        CHECK (completion_reason IS NULL OR completion_reason IN ('ALL_SUBMITTED', 'OWNER_EARLY_CLOSE')),
    CONSTRAINT vote_sessions_sticker_allowance_check
        CHECK (sticker_allowance >= 1),
    CONSTRAINT vote_sessions_selection_count_check
        CHECK (selection_count >= 1),
    CONSTRAINT vote_sessions_candidate_count_check
        CHECK (candidate_count >= 1),
    -- 지급 개수와 선정 개수는 후보 수를 넘을 수 없다.
    CONSTRAINT vote_sessions_allowance_within_candidates_check
        CHECK (sticker_allowance <= candidate_count),
    CONSTRAINT vote_sessions_selection_within_candidates_check
        CHECK (selection_count <= candidate_count)
);

-- 여행방당 진행 중(DRAFT/OPEN) 세션은 최대 1개다. 완료된 세션은 여러 개 남을 수 있다.
CREATE UNIQUE INDEX uq_vote_sessions_trip_active
    ON voting.vote_sessions (trip_id)
    WHERE status IN ('DRAFT', 'OPEN');
CREATE INDEX idx_vote_sessions_trip_created
    ON voting.vote_sessions (trip_id, created_at DESC);
CREATE INDEX idx_vote_sessions_status
    ON voting.vote_sessions (status);

-- ------------------------------------------------------------
-- voting.vote_session_candidates: 투표 시작 시점 추천 후보 snapshot.
-- sticker_count는 제출 transaction 안에서 갱신되는 집계값이다.
-- ------------------------------------------------------------
CREATE TABLE voting.vote_session_candidates (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_session_id   uuid NOT NULL REFERENCES voting.vote_sessions(id) ON DELETE CASCADE,
    sort_order        integer NOT NULL,
    place_provider    varchar(40) NOT NULL,
    external_place_id varchar(120) NOT NULL,
    place_name        varchar(240) NOT NULL,
    address           text,
    lat               numeric(10, 7),
    lng               numeric(10, 7),
    thumbnail_url     text,
    category          varchar(80),
    sticker_count     integer NOT NULL DEFAULT 0,
    selected          boolean NOT NULL DEFAULT false,
    selected_rank     integer,
    created_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT vote_session_candidates_sticker_count_check CHECK (sticker_count >= 0)
);

CREATE UNIQUE INDEX uq_vote_session_candidates_place
    ON voting.vote_session_candidates (vote_session_id, place_provider, external_place_id);
CREATE UNIQUE INDEX uq_vote_session_candidates_sort_order
    ON voting.vote_session_candidates (vote_session_id, sort_order);

-- ------------------------------------------------------------
-- voting.vote_session_participants: 투표 시작 시점에 확정된 참여자.
-- 첫 진입 여부를 단순 boolean으로 두지 않고 세션별 참여 상태로 관리한다.
-- 세션 시작 이후 합류한 멤버는 참여자로 추가하지 않는다.
-- ------------------------------------------------------------
CREATE TABLE voting.vote_session_participants (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_session_id    uuid NOT NULL REFERENCES voting.vote_sessions(id) ON DELETE CASCADE,
    user_id            uuid NOT NULL REFERENCES auth.users(id),
    status             varchar(20) NOT NULL DEFAULT 'NOT_STARTED',
    sticker_allowance  integer NOT NULL,
    used_sticker_count integer NOT NULL DEFAULT 0,
    submitted_at       timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT vote_session_participants_status_check
        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED')),
    CONSTRAINT vote_session_participants_used_sticker_check
        CHECK (used_sticker_count >= 0 AND used_sticker_count <= sticker_allowance)
);

CREATE UNIQUE INDEX uq_vote_session_participants_user
    ON voting.vote_session_participants (vote_session_id, user_id);
CREATE INDEX idx_vote_session_participants_status
    ON voting.vote_session_participants (vote_session_id, status);
CREATE INDEX idx_vote_session_participants_user
    ON voting.vote_session_participants (user_id);

-- ------------------------------------------------------------
-- voting.vote_stickers: 참여자가 후보에 붙인 스티커.
-- 같은 관광지에 여러 스티커를 몰아붙일 수 있으므로 row를 여러 개 만들지 않고
-- (참여자, 후보)당 한 row에 개수를 저장한다. 이 개수는 취향 반영에도 그대로 사용된다.
-- ------------------------------------------------------------
CREATE TABLE voting.vote_stickers (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_session_id uuid NOT NULL REFERENCES voting.vote_sessions(id) ON DELETE CASCADE,
    participant_id  uuid NOT NULL REFERENCES voting.vote_session_participants(id) ON DELETE CASCADE,
    candidate_id    uuid NOT NULL REFERENCES voting.vote_session_candidates(id) ON DELETE CASCADE,
    sticker_count   integer NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT vote_stickers_count_check CHECK (sticker_count >= 1)
);

CREATE UNIQUE INDEX uq_vote_stickers_participant_candidate
    ON voting.vote_stickers (participant_id, candidate_id);
CREATE INDEX idx_vote_stickers_session_candidate
    ON voting.vote_stickers (vote_session_id, candidate_id);

-- ------------------------------------------------------------
-- voting.vote_session_itinerary_links: 선정 결과를 일정에 반영한 기록.
-- (세션, 후보) 복합 PK가 일정 추가의 멱등성 근거다. 재시도해도 같은 후보가 두 번 추가되지 않는다.
-- ------------------------------------------------------------
CREATE TABLE voting.vote_session_itinerary_links (
    vote_session_id   uuid NOT NULL REFERENCES voting.vote_sessions(id) ON DELETE CASCADE,
    candidate_id      uuid NOT NULL REFERENCES voting.vote_session_candidates(id) ON DELETE CASCADE,
    itinerary_item_id uuid,
    outcome           varchar(30) NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (vote_session_id, candidate_id),
    CONSTRAINT vote_session_itinerary_links_outcome_check
        CHECK (outcome IN ('ADDED', 'SKIPPED_DUPLICATE'))
);

CREATE INDEX idx_vote_session_itinerary_links_item
    ON voting.vote_session_itinerary_links (itinerary_item_id)
    WHERE itinerary_item_id IS NOT NULL;
