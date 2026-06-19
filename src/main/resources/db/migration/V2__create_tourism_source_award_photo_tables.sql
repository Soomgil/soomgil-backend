CREATE TABLE tourism_source.photo_contests (
	id uuid PRIMARY KEY,
	provider varchar(80) NOT NULL DEFAULT 'KTO_CONTENTS_LAB',
	contest_name varchar(200) NOT NULL,
	award_year integer NOT NULL,
	contest_round varchar(80),
	source_url text,
	created_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE tourism_source.contest_award_photos (
	id uuid PRIMARY KEY,
	contest_id uuid,
	award_year integer NOT NULL,
	award_name varchar(160),
	award_rank integer NOT NULL DEFAULT 999,
	title varchar(240),
	photographer_name varchar(120),
	original_file_name varchar(500) NOT NULL,
	extracted_region_text varchar(240),
	storage_provider varchar(40) NOT NULL DEFAULT 'S3',
	bucket varchar(120),
	object_key text NOT NULL,
	public_url text,
	mime_type varchar(120),
	byte_size bigint,
	width integer,
	height integer,
	checksum_sha256 varchar(128),
	upload_status varchar(30) NOT NULL DEFAULT 'DOWNLOADED',
	rights_status varchar(30) NOT NULL DEFAULT 'PENDING_REVIEW',
	license_note text,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_contest_award_photos_contest
		FOREIGN KEY (contest_id)
		REFERENCES tourism_source.photo_contests (id)
		ON DELETE SET NULL
);

CREATE TABLE tourism_source.contest_award_photo_matches (
	id uuid PRIMARY KEY,
	photo_id uuid NOT NULL,
	attraction_no integer,
	sido_code integer,
	gugun_code integer,
	match_scope varchar(30) NOT NULL,
	match_status varchar(30) NOT NULL,
	match_method varchar(40) NOT NULL,
	confidence numeric(5, 4),
	is_selected boolean NOT NULL DEFAULT false,
	rationale text,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_contest_award_photo_matches_photo
		FOREIGN KEY (photo_id)
		REFERENCES tourism_source.contest_award_photos (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_contest_award_photo_matches_attraction
		FOREIGN KEY (attraction_no)
		REFERENCES tourism_source.attractions (no)
		ON DELETE CASCADE
);

CREATE INDEX idx_photo_contests_award_year
	ON tourism_source.photo_contests (award_year);

CREATE INDEX idx_contest_award_photos_contest
	ON tourism_source.contest_award_photos (contest_id);

CREATE INDEX idx_contest_award_photos_public_status
	ON tourism_source.contest_award_photos (upload_status, rights_status, award_year, award_rank);

CREATE INDEX idx_contest_award_photo_matches_attraction
	ON tourism_source.contest_award_photo_matches (attraction_no, match_scope, match_status, is_selected);

CREATE INDEX idx_contest_award_photo_matches_region
	ON tourism_source.contest_award_photo_matches (sido_code, gugun_code, match_scope, match_status, is_selected);
