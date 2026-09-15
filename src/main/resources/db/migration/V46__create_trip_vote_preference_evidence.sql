-- ============================================================
-- Preference: 투표 스티커를 개인 취향에 반영하기 위한 TRIP_VOTE 출처
-- ============================================================
-- 스와이프 최종 반응(preference.user_place_reactions)과 이벤트 로그(user_swipe_events)는
-- 건드리지 않는다. LIKE/NOPE/SUPER_LIKE 의미, 저장 장소 정책, 반응 되돌리기 로직을 오염시키지 않기 위해
-- 투표 근거는 완전히 별도 테이블에 기록하고 projection에만 가산한다.

-- ------------------------------------------------------------
-- preference.user_place_vote_evidences
-- (vote_session_id, user_id, provider, external_place_id) unique가 멱등성의 근거다.
-- 같은 투표 제출을 재시도해도 취향 근거가 중복 반영되지 않는다.
-- ------------------------------------------------------------
CREATE TABLE preference.user_place_vote_evidences (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_session_id         uuid NOT NULL,
    user_id                 uuid NOT NULL REFERENCES auth.users(id),
    provider                varchar(40) NOT NULL,
    external_place_id       varchar(120) NOT NULL,
    sticker_count           integer NOT NULL,
    source                  varchar(20) NOT NULL DEFAULT 'TRIP_VOTE',
    place_tag_enrichment_id uuid,
    evidence_units          numeric(16, 8) NOT NULL,
    calculation_version     varchar(80) NOT NULL,
    applied_at              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_place_vote_evidences_sticker_count_check CHECK (sticker_count >= 1),
    CONSTRAINT user_place_vote_evidences_source_check CHECK (source = 'TRIP_VOTE')
);

CREATE UNIQUE INDEX uq_user_place_vote_evidences_session_user_place
    ON preference.user_place_vote_evidences (vote_session_id, user_id, provider, external_place_id);
CREATE INDEX idx_user_place_vote_evidences_user
    ON preference.user_place_vote_evidences (user_id);
CREATE INDEX idx_user_place_vote_evidences_place
    ON preference.user_place_vote_evidences (provider, external_place_id);

COMMENT ON TABLE preference.user_place_vote_evidences IS
    '여행 방 투표에서 스티커를 붙인 장소의 취향 근거. 스와이프 LIKE/NOPE/SUPER_LIKE와 구분되는 TRIP_VOTE 출처다.';

-- ------------------------------------------------------------
-- user_preference_tag_weights에 투표 근거 감사 컬럼을 추가한다.
-- positive_evidence에도 같은 값이 가산되므로 기존 추천 점수 계산 공식은 변경하지 않는다.
-- 이 두 컬럼은 "투표가 얼마나 기여했는지"를 되짚기 위한 감사/디버깅용이다.
-- ------------------------------------------------------------
ALTER TABLE preference.user_preference_tag_weights
    ADD COLUMN vote_evidence numeric(16, 8) NOT NULL DEFAULT 0,
    ADD COLUMN vote_place_count integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN preference.user_preference_tag_weights.vote_evidence IS
    'TRIP_VOTE 출처로 가산된 누적 근거. positive_evidence에도 함께 반영되어 있다.';
COMMENT ON COLUMN preference.user_preference_tag_weights.vote_place_count IS
    'TRIP_VOTE 출처로 이 태그에 기여한 장소 수.';
