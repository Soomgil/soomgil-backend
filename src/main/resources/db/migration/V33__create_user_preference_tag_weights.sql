CREATE TABLE preference.user_preference_tag_weights (
	user_id uuid NOT NULL,
	tag_id uuid NOT NULL,
	positive_evidence numeric(16, 8) NOT NULL DEFAULT 0,
	negative_evidence numeric(16, 8) NOT NULL DEFAULT 0,
	preference_score numeric(8, 6) NOT NULL DEFAULT 0.5,
	like_count integer NOT NULL DEFAULT 0,
	super_like_count integer NOT NULL DEFAULT 0,
	nope_count integer NOT NULL DEFAULT 0,
	calculation_version varchar(80) NOT NULL DEFAULT 'preference-evidence-v1',
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	PRIMARY KEY (user_id, tag_id),
	CONSTRAINT fk_user_preference_tag_weights_tag
		FOREIGN KEY (tag_id) REFERENCES preference.preference_tags (id),
	CONSTRAINT chk_user_preference_tag_weights_evidence
		CHECK (positive_evidence >= 0 AND negative_evidence >= 0),
	CONSTRAINT chk_user_preference_tag_weights_score
		CHECK (preference_score BETWEEN 0 AND 1),
	CONSTRAINT chk_user_preference_tag_weights_counts
		CHECK (like_count >= 0 AND super_like_count >= 0 AND nope_count >= 0)
);

CREATE INDEX idx_user_preference_tag_weights_tag
	ON preference.user_preference_tag_weights (tag_id);

CREATE INDEX idx_user_preference_tag_weights_score
	ON preference.user_preference_tag_weights (preference_score);
