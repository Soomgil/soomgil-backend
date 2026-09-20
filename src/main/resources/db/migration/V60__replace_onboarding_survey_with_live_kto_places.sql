UPDATE preference.onboarding_survey_versions
SET status = 'RETIRED'
WHERE status = 'ACTIVE';

INSERT INTO preference.onboarding_survey_versions (
	id, code, required_place_count, status, activated_at
)
VALUES (
	'8f4a2b66-e120-45d2-9bc7-5b7708de1002',
	'nationwide-diversity-v2',
	10,
	'ACTIVE',
	now()
);

-- 장소 선택만 version에 고정한다. 제목, 주소, 설명, 사진은 KTO API 응답을 사용한다.
INSERT INTO preference.onboarding_survey_places (
	survey_version_id, provider, external_place_id, sort_order
)
VALUES
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '126508', 1),  -- 경복궁
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '126435', 2),  -- 성산일출봉
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '126081', 3),  -- 해운대해수욕장
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '1934593', 4), -- 국립현대미술관 서울
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '127797', 5),  -- 에버랜드
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '126730', 6),  -- 순천만습지
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '1997221', 7), -- 부산 감천문화마을
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '2660802', 8), -- 오설록 티뮤지엄
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '126166', 9),  -- 경주 불국사
	('8f4a2b66-e120-45d2-9bc7-5b7708de1002', 'KTO', '1590323', 10); -- 통인시장
