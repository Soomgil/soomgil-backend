-- 성공한 원천 응답은 자동 만료 없이 재사용한다. 실패는 재시도 가능 시각만 기록한다.
CREATE TABLE tourism_source.kto_responses (
    request_key text PRIMARY KEY,
    payload jsonb,
    fetched_at timestamptz,
    retry_after timestamptz,
    failure_count integer NOT NULL DEFAULT 0,
    CHECK (request_key NOT ILIKE '%servicekey%')
);
CREATE INDEX kto_responses_fetched_at ON tourism_source.kto_responses(fetched_at);
-- 원본 URL을 기준으로 seed 및 API 수집 이미지의 중복을 방지한다.
CREATE INDEX attraction_images_source_url ON tourism_source.attraction_images(attraction_no, public_url);
