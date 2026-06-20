CREATE TABLE preference.tag_statistic_runs (
	id uuid PRIMARY KEY,
	source varchar(30) NOT NULL,
	status varchar(30) NOT NULL DEFAULT 'PENDING',
	alpha numeric(8, 4) NOT NULL DEFAULT 100,
	global_positive_rate numeric(8, 6),
	total_reaction_count bigint NOT NULL DEFAULT 0,
	positive_reaction_count bigint NOT NULL DEFAULT 0,
	generator_version varchar(80),
	error_message text,
	is_serving boolean NOT NULL DEFAULT false,
	started_at timestamp with time zone,
	completed_at timestamp with time zone,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT chk_tag_statistic_runs_source
		CHECK (source IN ('AI_ONLY_DEFAULT', 'SYNTHETIC_PERSONA', 'REAL_USER')),
	CONSTRAINT chk_tag_statistic_runs_status
		CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'ARCHIVED')),
	CONSTRAINT chk_tag_statistic_runs_alpha CHECK (alpha > 0),
	CONSTRAINT chk_tag_statistic_runs_global_positive_rate
		CHECK (global_positive_rate IS NULL OR global_positive_rate BETWEEN 0 AND 1)
);

CREATE INDEX idx_tag_statistic_runs_source
	ON preference.tag_statistic_runs (source);

CREATE INDEX idx_tag_statistic_runs_status
	ON preference.tag_statistic_runs (status);

CREATE UNIQUE INDEX uq_tag_statistic_runs_one_serving
	ON preference.tag_statistic_runs (is_serving)
	WHERE is_serving = true;

CREATE TABLE preference.tag_statistics (
	run_id uuid NOT NULL,
	tag_id uuid NOT NULL,
	preference_discrimination numeric(8, 6) NOT NULL,
	smoothed_positive_rate numeric(8, 6),
	positive_count bigint NOT NULL DEFAULT 0,
	reaction_count bigint NOT NULL DEFAULT 0,
	calculated_at timestamp with time zone NOT NULL DEFAULT now(),
	PRIMARY KEY (run_id, tag_id),
	CONSTRAINT fk_tag_statistics_run
		FOREIGN KEY (run_id) REFERENCES preference.tag_statistic_runs (id) ON DELETE CASCADE,
	CONSTRAINT fk_tag_statistics_tag
		FOREIGN KEY (tag_id) REFERENCES preference.preference_tags (id),
	CONSTRAINT chk_tag_statistics_discrimination
		CHECK (preference_discrimination BETWEEN 0 AND 1),
	CONSTRAINT chk_tag_statistics_smoothed_positive_rate
		CHECK (smoothed_positive_rate IS NULL OR smoothed_positive_rate BETWEEN 0 AND 1),
	CONSTRAINT chk_tag_statistics_counts
		CHECK (positive_count >= 0 AND reaction_count >= positive_count)
);

CREATE INDEX idx_tag_statistics_tag
	ON preference.tag_statistics (tag_id);

CREATE INDEX idx_tag_statistics_discrimination
	ON preference.tag_statistics (preference_discrimination);

ALTER TABLE preference.place_tag_enrichments
	ADD CONSTRAINT fk_place_tag_enrichments_statistic_run
	FOREIGN KEY (tag_statistic_run_id)
	REFERENCES preference.tag_statistic_runs (id);

ALTER TABLE preference.place_tag_enrichment_tags
	ADD CONSTRAINT fk_place_tag_enrichment_tags_statistic_run
	FOREIGN KEY (tag_statistic_run_id)
	REFERENCES preference.tag_statistic_runs (id);
