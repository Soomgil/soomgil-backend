CREATE TABLE preference.user_place_reactions (
	id uuid PRIMARY KEY,
	user_id uuid NOT NULL,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	reaction varchar(20) NOT NULL,
	reaction_count integer NOT NULL DEFAULT 1,
	first_reacted_at timestamp with time zone NOT NULL DEFAULT now(),
	last_reacted_at timestamp with time zone NOT NULL DEFAULT now(),
	source_modified_at timestamp with time zone,
	place_tag_enrichment_id uuid,
	CONSTRAINT uq_user_place_reactions_place
		UNIQUE (user_id, provider, external_place_id),
	CONSTRAINT fk_user_place_reactions_enrichment
		FOREIGN KEY (place_tag_enrichment_id)
		REFERENCES preference.place_tag_enrichments (id),
	CONSTRAINT chk_user_place_reactions_reaction
		CHECK (reaction IN ('LIKE', 'NOPE', 'SUPER_LIKE'))
);

CREATE INDEX idx_user_place_reactions_user
	ON preference.user_place_reactions (user_id);

CREATE INDEX idx_user_place_reactions_reaction
	ON preference.user_place_reactions (reaction);

CREATE INDEX idx_user_place_reactions_place
	ON preference.user_place_reactions (provider, external_place_id);

CREATE TABLE preference.user_swipe_events (
	id bigserial PRIMARY KEY,
	user_id uuid NOT NULL,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	reaction varchar(20) NOT NULL,
	previous_reaction varchar(20),
	feed_context jsonb,
	source_modified_at timestamp with time zone,
	place_tag_enrichment_id uuid,
	occurred_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_user_swipe_events_enrichment
		FOREIGN KEY (place_tag_enrichment_id)
		REFERENCES preference.place_tag_enrichments (id),
	CONSTRAINT chk_user_swipe_events_reaction
		CHECK (reaction IN ('LIKE', 'NOPE', 'SUPER_LIKE')),
	CONSTRAINT chk_user_swipe_events_previous_reaction
		CHECK (previous_reaction IS NULL OR previous_reaction IN ('LIKE', 'NOPE', 'SUPER_LIKE'))
);

CREATE INDEX idx_user_swipe_events_user
	ON preference.user_swipe_events (user_id);

CREATE INDEX idx_user_swipe_events_place
	ON preference.user_swipe_events (provider, external_place_id);

CREATE INDEX idx_user_swipe_events_reaction
	ON preference.user_swipe_events (reaction);

CREATE INDEX idx_user_swipe_events_occurred_at
	ON preference.user_swipe_events (occurred_at);
