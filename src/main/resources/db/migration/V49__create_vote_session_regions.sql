-- ============================================================
-- Voting: 투표 세션이 후보를 뽑은 지역 snapshot
-- ============================================================
-- 방장은 투표를 시작할 때 이번 투표의 지역을 직접 고를 수 있다. 고르지 않으면 여행방에 등록된
-- 지역을 쓴다. 어느 쪽이든 시작 시점의 지역을 세션에 고정해, 이후 여행방 지역이 바뀌어도
-- "이 후보가 어디서 나왔는지"를 결과 화면과 감사 기록에서 그대로 보여줄 수 있게 한다.

-- ------------------------------------------------------------
-- voting.vote_session_regions: 세션당 지역 목록. 이름도 함께 고정한다.
-- ------------------------------------------------------------
CREATE TABLE voting.vote_session_regions (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_session_id     uuid NOT NULL REFERENCES voting.vote_sessions(id) ON DELETE CASCADE,
    legal_region_code   varchar(10) NOT NULL,
    region_name         varchar(200) NOT NULL,
    sort_order          integer NOT NULL DEFAULT 0,
    created_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT vote_session_regions_code_length_check
        CHECK (char_length(legal_region_code) = 10),
    CONSTRAINT vote_session_regions_session_code_unique
        UNIQUE (vote_session_id, legal_region_code)
);

CREATE INDEX vote_session_regions_session_order_idx
    ON voting.vote_session_regions (vote_session_id, sort_order);

COMMENT ON TABLE voting.vote_session_regions IS
    '투표 세션이 후보를 뽑은 법정동 지역 snapshot. 방장이 고른 지역 또는 시작 시점의 여행방 지역.';
COMMENT ON COLUMN voting.vote_session_regions.region_name IS
    '시작 시점의 지역 이름. geo.legal_regions에서 못 찾으면 코드를 그대로 저장한다.';
