CREATE TABLE preference.place_tag_enrichments (
	id uuid PRIMARY KEY,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	source_modified_at timestamp with time zone,
	source_hash varchar(128),
	status varchar(30) NOT NULL DEFAULT 'PENDING',
	model_provider varchar(80),
	model_name varchar(120),
	prompt_version varchar(80),
	tag_dictionary_version varchar(80),
	selection_policy_version varchar(80),
	tag_statistic_run_id uuid,
	candidate_count integer NOT NULL DEFAULT 0,
	selected_count integer NOT NULL DEFAULT 0,
	error_message text,
	enriched_at timestamp with time zone,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT chk_place_tag_enrichments_status
		CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'STALE'))
);

CREATE INDEX idx_place_tag_enrichments_place
	ON preference.place_tag_enrichments (provider, external_place_id);

CREATE INDEX idx_place_tag_enrichments_source_hash
	ON preference.place_tag_enrichments (source_hash);

CREATE INDEX idx_place_tag_enrichments_status
	ON preference.place_tag_enrichments (status);

CREATE INDEX idx_place_tag_enrichments_statistic_run
	ON preference.place_tag_enrichments (tag_statistic_run_id);

CREATE INDEX idx_place_tag_enrichments_enriched_at
	ON preference.place_tag_enrichments (enriched_at);

CREATE TABLE preference.place_tag_enrichment_candidates (
	id uuid PRIMARY KEY,
	enrichment_id uuid NOT NULL,
	candidate_code varchar(80) NOT NULL,
	matched_tag_id uuid,
	confidence numeric(5, 4),
	weight numeric(8, 4),
	selection_score numeric(8, 6),
	status varchar(40) NOT NULL,
	rationale text,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_place_tag_enrichment_candidates_enrichment
		FOREIGN KEY (enrichment_id)
		REFERENCES preference.place_tag_enrichments (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_place_tag_enrichment_candidates_tag
		FOREIGN KEY (matched_tag_id)
		REFERENCES preference.preference_tags (id),
	CONSTRAINT chk_place_tag_enrichment_candidates_status
		CHECK (
			status IN (
				'SELECTED',
				'REJECTED_OUT_OF_DICTIONARY',
				'REJECTED_LOW_CONFIDENCE',
				'REJECTED_LOW_SCORE',
				'REJECTED_DUPLICATE',
				'REJECTED_LIMIT'
			)
		)
);

CREATE INDEX idx_place_tag_enrichment_candidates_enrichment
	ON preference.place_tag_enrichment_candidates (enrichment_id);

CREATE INDEX idx_place_tag_enrichment_candidates_code
	ON preference.place_tag_enrichment_candidates (candidate_code);

CREATE INDEX idx_place_tag_enrichment_candidates_matched_tag
	ON preference.place_tag_enrichment_candidates (matched_tag_id);

CREATE INDEX idx_place_tag_enrichment_candidates_status
	ON preference.place_tag_enrichment_candidates (status);

CREATE TABLE preference.place_tag_enrichment_tags (
	enrichment_id uuid NOT NULL,
	tag_id uuid NOT NULL,
	confidence numeric(5, 4) NOT NULL,
	weight numeric(8, 4) NOT NULL,
	preference_discrimination_snapshot numeric(8, 6),
	selection_score numeric(8, 6),
	rank_order integer NOT NULL,
	tag_statistic_run_id uuid,
	rationale text,
	PRIMARY KEY (enrichment_id, tag_id),
	CONSTRAINT fk_place_tag_enrichment_tags_enrichment
		FOREIGN KEY (enrichment_id)
		REFERENCES preference.place_tag_enrichments (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_place_tag_enrichment_tags_tag
		FOREIGN KEY (tag_id)
		REFERENCES preference.preference_tags (id)
);

CREATE INDEX idx_place_tag_enrichment_tags_tag
	ON preference.place_tag_enrichment_tags (tag_id);

CREATE INDEX idx_place_tag_enrichment_tags_statistic_run
	ON preference.place_tag_enrichment_tags (tag_statistic_run_id);

CREATE INDEX idx_place_tag_enrichment_tags_rank
	ON preference.place_tag_enrichment_tags (rank_order);
