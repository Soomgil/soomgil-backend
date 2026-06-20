CREATE TABLE preference.user_saved_places (
	id uuid PRIMARY KEY,
	user_id uuid NOT NULL,
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	deleted_at timestamp with time zone,
	CONSTRAINT uq_user_saved_places_place
		UNIQUE (user_id, provider, external_place_id)
);

CREATE INDEX idx_user_saved_places_place
	ON preference.user_saved_places (provider, external_place_id);

CREATE INDEX idx_user_saved_places_deleted_at
	ON preference.user_saved_places (deleted_at);
