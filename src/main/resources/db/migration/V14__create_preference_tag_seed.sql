CREATE SCHEMA IF NOT EXISTS preference;

CREATE TABLE preference.preference_tags (
	id uuid PRIMARY KEY,
	code varchar(80) NOT NULL UNIQUE,
	display_name varchar(120) NOT NULL,
	group_code varchar(80) NOT NULL,
	tag_type varchar(20) NOT NULL DEFAULT 'TAG',
	parent_tag_id uuid,
	description text,
	is_selectable boolean NOT NULL DEFAULT true,
	is_active boolean NOT NULL DEFAULT true,
	dictionary_version varchar(80) NOT NULL DEFAULT 'preference-tags-v1',
	sort_order integer,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_preference_tags_parent
		FOREIGN KEY (parent_tag_id)
		REFERENCES preference.preference_tags (id),
	CONSTRAINT chk_preference_tags_tag_type
		CHECK (tag_type IN ('GROUP', 'TAG'))
);

CREATE INDEX idx_preference_tags_group_code
	ON preference.preference_tags (group_code);

CREATE INDEX idx_preference_tags_tag_type
	ON preference.preference_tags (tag_type);

CREATE INDEX idx_preference_tags_parent
	ON preference.preference_tags (parent_tag_id);

CREATE INDEX idx_preference_tags_selectable
	ON preference.preference_tags (is_selectable);

CREATE INDEX idx_preference_tags_active
	ON preference.preference_tags (is_active);

CREATE INDEX idx_preference_tags_dictionary_version
	ON preference.preference_tags (dictionary_version);

WITH seed (
	code,
	display_name,
	group_code,
	tag_type,
	is_selectable,
	sort_order
) AS (
	VALUES
		('nature_scene', '자연/경관', 'nature_scene', 'GROUP', false, 10),
		('nature', '자연', 'nature_scene', 'TAG', true, 11),
		('park', '공원', 'nature_scene', 'TAG', true, 12),
		('arboretum', '수목원', 'nature_scene', 'TAG', true, 13),
		('garden', '정원', 'nature_scene', 'TAG', true, 14),
		('forest', '숲', 'nature_scene', 'TAG', true, 15),
		('mountain', '산', 'nature_scene', 'TAG', true, 16),
		('coast', '바다/해안', 'nature_scene', 'TAG', true, 17),
		('island', '섬', 'nature_scene', 'TAG', true, 18),
		('lake_pond', '연못/호수', 'nature_scene', 'TAG', true, 19),
		('waterfront', '수변경관', 'nature_scene', 'TAG', true, 20),
		('valley_stream', '계곡', 'nature_scene', 'TAG', true, 21),
		('waterfall', '폭포', 'nature_scene', 'TAG', true, 22),
		('flower_plant', '꽃/식물', 'nature_scene', 'TAG', true, 23),
		('scenic_view', '풍경좋은', 'nature_scene', 'TAG', true, 24),
		('rural_landscape', '농촌풍경', 'nature_scene', 'TAG', true, 25),
		('fishing_village', '어촌/포구', 'nature_scene', 'TAG', true, 26),
		('night_view', '야경', 'nature_scene', 'TAG', true, 27),
		('sunset', '노을', 'nature_scene', 'TAG', true, 28),
		('stargazing', '별보기', 'nature_scene', 'TAG', true, 29),
		('autumn_foliage', '단풍', 'nature_scene', 'TAG', true, 30),
		('snow_scene', '설경', 'nature_scene', 'TAG', true, 31),
		('history_culture', '역사/문화', 'history_culture', 'GROUP', false, 100),
		('history', '역사', 'history_culture', 'TAG', true, 101),
		('traditional', '전통적인', 'history_culture', 'TAG', true, 102),
		('traditional_architecture', '전통건축', 'history_culture', 'TAG', true, 103),
		('palace_fortress', '궁궐/성곽', 'history_culture', 'TAG', true, 104),
		('temple_shrine', '사찰/종교공간', 'history_culture', 'TAG', true, 105),
		('heritage_site', '문화유산', 'history_culture', 'TAG', true, 106),
		('local_culture', '지역문화', 'history_culture', 'TAG', true, 107),
		('museum', '박물관', 'history_culture', 'TAG', true, 108),
		('gallery_exhibition', '전시/미술', 'history_culture', 'TAG', true, 109),
		('science_education', '과학/교육', 'history_culture', 'TAG', true, 110),
		('cultural_space', '문화공간', 'history_culture', 'TAG', true, 111),
		('performance_venue', '공연공간', 'history_culture', 'TAG', true, 112),
		('architecture', '건축', 'history_culture', 'TAG', true, 113),
		('industrial_heritage', '산업유산/재생공간', 'history_culture', 'TAG', true, 114),
		('activity', '활동', 'activity', 'GROUP', false, 200),
		('walking', '산책', 'activity', 'TAG', true, 201),
		('hiking', '등산', 'activity', 'TAG', true, 202),
		('cycling', '자전거', 'activity', 'TAG', true, 203),
		('photo_spot', '사진명소', 'activity', 'TAG', true, 204),
		('viewing', '관람', 'activity', 'TAG', true, 205),
		('hands_on_experience', '체험', 'activity', 'TAG', true, 206),
		('learning', '학습', 'activity', 'TAG', true, 207),
		('performance_viewing', '공연관람', 'activity', 'TAG', true, 208),
		('picnic', '피크닉', 'activity', 'TAG', true, 209),
		('leisure_activity', '레저활동', 'activity', 'TAG', true, 210),
		('water_activity', '수상활동', 'activity', 'TAG', true, 211),
		('camping', '캠핑', 'activity', 'TAG', true, 212),
		('hot_spring', '온천', 'activity', 'TAG', true, 213),
		('animal_viewing', '동물관람', 'activity', 'TAG', true, 214),
		('rides', '놀이기구', 'activity', 'TAG', true, 215),
		('festival', '축제', 'activity', 'TAG', true, 216),
		('bookshop', '서점', 'activity', 'TAG', true, 217),
		('mood', '분위기', 'mood', 'GROUP', false, 300),
		('healing', '힐링', 'mood', 'TAG', true, 301),
		('quiet', '조용한', 'mood', 'TAG', true, 302),
		('lively', '활기찬', 'mood', 'TAG', true, 303),
		('romantic', '로맨틱', 'mood', 'TAG', true, 304),
		('educational', '교육적인', 'mood', 'TAG', true, 305),
		('active', '활동적인', 'mood', 'TAG', true, 306),
		('unique', '이색적인', 'mood', 'TAG', true, 307),
		('nostalgic', '레트로/향수', 'mood', 'TAG', true, 308),
		('artistic', '예술적인', 'mood', 'TAG', true, 309),
		('open_feeling', '개방감', 'mood', 'TAG', true, 310),
		('modern', '현대적인', 'mood', 'TAG', true, 311),
		('futuristic', '미래적인', 'mood', 'TAG', true, 312),
		('space_context', '공간/환경', 'space_context', 'GROUP', false, 400),
		('outdoor', '야외', 'space_context', 'TAG', true, 401),
		('indoor', '실내', 'space_context', 'TAG', true, 402),
		('urban', '도심', 'space_context', 'TAG', true, 403),
		('nature_escape', '도심속자연', 'space_context', 'TAG', true, 404),
		('landmark', '랜드마크', 'space_context', 'TAG', true, 405),
		('theme_park', '테마파크', 'space_context', 'TAG', true, 406),
		('observatory', '전망공간', 'space_context', 'TAG', true, 407),
		('street_alley', '거리/골목', 'space_context', 'TAG', true, 408)
)
INSERT INTO preference.preference_tags (
	id,
	code,
	display_name,
	group_code,
	tag_type,
	parent_tag_id,
	is_selectable,
	is_active,
	dictionary_version,
	sort_order
)
SELECT
	md5('preference-tags-v1:' || code)::uuid,
	code,
	display_name,
	group_code,
	tag_type,
	CASE
		WHEN tag_type = 'TAG' THEN md5('preference-tags-v1:' || group_code)::uuid
		ELSE NULL
	END,
	is_selectable,
	true,
	'preference-tags-v1',
	sort_order
FROM seed;
