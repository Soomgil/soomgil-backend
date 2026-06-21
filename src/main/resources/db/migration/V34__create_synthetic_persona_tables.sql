CREATE TABLE preference.synthetic_personas (
	id uuid PRIMARY KEY,
	persona_key varchar(80) NOT NULL,
	display_name varchar(120) NOT NULL,
	description text NOT NULL,
	generator_version varchar(80) NOT NULL,
	seed bigint NOT NULL,
	noise_rate numeric(5, 4) NOT NULL,
	is_active boolean NOT NULL DEFAULT true,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT uq_synthetic_personas_version_key UNIQUE (generator_version, persona_key),
	CONSTRAINT chk_synthetic_personas_noise_rate CHECK (noise_rate BETWEEN 0 AND 0.05)
);

CREATE INDEX idx_synthetic_personas_version_active
	ON preference.synthetic_personas (generator_version, is_active);

CREATE TABLE preference.synthetic_persona_tag_preferences (
	persona_id uuid NOT NULL,
	tag_id uuid NOT NULL,
	preference_type varchar(30) NOT NULL,
	preference_strength numeric(8, 4) NOT NULL,
	PRIMARY KEY (persona_id, tag_id),
	CONSTRAINT fk_synthetic_persona_tag_preferences_persona
		FOREIGN KEY (persona_id) REFERENCES preference.synthetic_personas (id) ON DELETE CASCADE,
	CONSTRAINT fk_synthetic_persona_tag_preferences_tag
		FOREIGN KEY (tag_id) REFERENCES preference.preference_tags (id),
	CONSTRAINT chk_synthetic_persona_tag_preferences_type
		CHECK (preference_type IN ('HARD_LIKE', 'HARD_DISLIKE', 'SOFT_LIKE', 'SOFT_DISLIKE', 'NEUTRAL'))
);

CREATE INDEX idx_synthetic_persona_tag_preferences_tag
	ON preference.synthetic_persona_tag_preferences (tag_id);

CREATE TABLE preference.synthetic_swipe_events (
	id bigserial PRIMARY KEY,
	persona_id uuid NOT NULL,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	reaction varchar(20) NOT NULL,
	place_tag_enrichment_id uuid,
	generator_version varchar(80) NOT NULL,
	seed bigint NOT NULL,
	persona_place_score numeric(10, 6),
	generated_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_synthetic_swipe_events_persona
		FOREIGN KEY (persona_id) REFERENCES preference.synthetic_personas (id) ON DELETE CASCADE,
	CONSTRAINT fk_synthetic_swipe_events_enrichment
		FOREIGN KEY (place_tag_enrichment_id) REFERENCES preference.place_tag_enrichments (id),
	CONSTRAINT uq_synthetic_swipe_events_generation
		UNIQUE (persona_id, provider, external_place_id, generator_version, seed),
	CONSTRAINT chk_synthetic_swipe_events_reaction
		CHECK (reaction IN ('LIKE', 'NOPE', 'SUPER_LIKE'))
);

CREATE INDEX idx_synthetic_swipe_events_persona
	ON preference.synthetic_swipe_events (persona_id);

CREATE INDEX idx_synthetic_swipe_events_place
	ON preference.synthetic_swipe_events (provider, external_place_id);

CREATE INDEX idx_synthetic_swipe_events_version_reaction
	ON preference.synthetic_swipe_events (generator_version, reaction);
