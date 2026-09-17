-- 자동 갱신 없이 수동 요청 및 원천 수정 시각만 기록한다.
ALTER TABLE tourism_source.kto_responses
    ADD COLUMN source_modified_at timestamptz,
    ADD COLUMN refresh_requested boolean NOT NULL DEFAULT false;
