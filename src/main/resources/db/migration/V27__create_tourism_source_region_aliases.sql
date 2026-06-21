CREATE TABLE tourism_source.region_aliases (
	id uuid PRIMARY KEY,
	alias varchar(120) NOT NULL,
	normalized_alias varchar(120) NOT NULL,
	sido_code integer,
	gugun_code integer,
	alias_type varchar(40) NOT NULL,
	is_active boolean NOT NULL DEFAULT true,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fk_region_aliases_sido
		FOREIGN KEY (sido_code)
		REFERENCES tourism_source.sidos (sido_code)
		ON DELETE CASCADE,
	CONSTRAINT fk_region_aliases_gugun
		FOREIGN KEY (sido_code, gugun_code)
		REFERENCES tourism_source.guguns (sido_code, gugun_code)
		ON DELETE CASCADE
);

CREATE INDEX idx_region_aliases_normalized_alias
	ON tourism_source.region_aliases (normalized_alias);

CREATE INDEX idx_region_aliases_region
	ON tourism_source.region_aliases (sido_code, gugun_code);

CREATE INDEX idx_region_aliases_active
	ON tourism_source.region_aliases (is_active);
