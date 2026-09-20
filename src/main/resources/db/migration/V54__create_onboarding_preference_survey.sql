ALTER TABLE auth.users
	ADD COLUMN IF NOT EXISTS onboarding_completed_at timestamp with time zone;

-- 이 기능 배포 전에 가입한 사용자는 기존 화면 흐름을 유지한다.
UPDATE auth.users
SET onboarding_completed_at = coalesce(onboarding_completed_at, now())
WHERE onboarding_completed_at IS NULL;

ALTER TABLE preference.user_place_reactions
	ADD COLUMN IF NOT EXISTS last_source varchar(30) NOT NULL DEFAULT 'HOME_BACKGROUND',
	ADD COLUMN IF NOT EXISTS last_source_resource_id varchar(160),
	ADD COLUMN IF NOT EXISTS evidence_multiplier numeric(4, 2) NOT NULL DEFAULT 1.0;

ALTER TABLE preference.user_place_reactions
	ADD CONSTRAINT chk_user_place_reactions_source
		CHECK (last_source IN ('ONBOARDING', 'HOME_BACKGROUND', 'TRIP_VOTE')),
	ADD CONSTRAINT chk_user_place_reactions_evidence_multiplier
		CHECK (evidence_multiplier BETWEEN 1.0 AND 3.0);

ALTER TABLE preference.user_swipe_events
	ADD COLUMN IF NOT EXISTS source varchar(30) NOT NULL DEFAULT 'HOME_BACKGROUND',
	ADD COLUMN IF NOT EXISTS source_resource_id varchar(160),
	ADD COLUMN IF NOT EXISTS evidence_multiplier numeric(4, 2) NOT NULL DEFAULT 1.0;

ALTER TABLE preference.user_swipe_events
	ADD CONSTRAINT chk_user_swipe_events_source
		CHECK (source IN ('ONBOARDING', 'HOME_BACKGROUND', 'TRIP_VOTE')),
	ADD CONSTRAINT chk_user_swipe_events_evidence_multiplier
		CHECK (evidence_multiplier BETWEEN 1.0 AND 3.0);

CREATE TABLE preference.onboarding_survey_versions (
	id uuid PRIMARY KEY,
	code varchar(80) NOT NULL UNIQUE,
	required_place_count integer NOT NULL DEFAULT 10,
	status varchar(20) NOT NULL DEFAULT 'DRAFT',
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	activated_at timestamp with time zone,
	CONSTRAINT chk_onboarding_survey_version_count CHECK (required_place_count = 10),
	CONSTRAINT chk_onboarding_survey_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED'))
);

CREATE UNIQUE INDEX uq_onboarding_survey_single_active
	ON preference.onboarding_survey_versions ((status))
	WHERE status = 'ACTIVE';

CREATE TABLE preference.onboarding_survey_places (
	survey_version_id uuid NOT NULL,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	sort_order integer NOT NULL,
	PRIMARY KEY (survey_version_id, provider, external_place_id),
	CONSTRAINT uq_onboarding_survey_place_order UNIQUE (survey_version_id, sort_order),
	CONSTRAINT fk_onboarding_survey_place_version
		FOREIGN KEY (survey_version_id)
		REFERENCES preference.onboarding_survey_versions (id),
	CONSTRAINT chk_onboarding_survey_place_order CHECK (sort_order BETWEEN 1 AND 10)
);

CREATE TABLE preference.user_onboarding_responses (
	user_id uuid NOT NULL,
	survey_version_id uuid NOT NULL,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	reaction varchar(20) NOT NULL,
	responded_at timestamp with time zone NOT NULL DEFAULT now(),
	PRIMARY KEY (user_id, survey_version_id, provider, external_place_id),
	CONSTRAINT fk_user_onboarding_response_user
		FOREIGN KEY (user_id) REFERENCES auth.users (id),
	CONSTRAINT fk_user_onboarding_response_place
		FOREIGN KEY (survey_version_id, provider, external_place_id)
		REFERENCES preference.onboarding_survey_places (survey_version_id, provider, external_place_id),
	CONSTRAINT chk_user_onboarding_response_reaction CHECK (reaction IN ('LIKE', 'NOPE'))
);

CREATE INDEX idx_user_onboarding_responses_survey
	ON preference.user_onboarding_responses (survey_version_id);

INSERT INTO preference.onboarding_survey_versions (
	id, code, required_place_count, status, activated_at
)
VALUES (
	'8f4a2b66-e120-45d2-9bc7-5b7708de1001',
	'jeju-diversity-v1',
	10,
	'ACTIVE',
	now()
);

-- 자연, 도심, 문화, 미식, 액티비티, 실내외 취향이 겹치지 않도록 구성한 첫 설문이다.
INSERT INTO preference.onboarding_survey_places (
	survey_version_id, provider, external_place_id, sort_order
)
VALUES
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '125445', 1),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126435', 2),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126461', 3),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126453', 4),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126472', 5),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126471', 6),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126436', 7),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126474', 8),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126470', 9),
	('8f4a2b66-e120-45d2-9bc7-5b7708de1001', 'KTO', '126454', 10);
