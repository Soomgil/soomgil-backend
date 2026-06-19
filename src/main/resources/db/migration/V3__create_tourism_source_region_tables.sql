CREATE TABLE tourism_source.sidos (
	sido_code integer PRIMARY KEY,
	sido_name varchar(120) NOT NULL
);

CREATE TABLE tourism_source.guguns (
	sido_code integer NOT NULL,
	gugun_code integer NOT NULL,
	gugun_name varchar(120) NOT NULL,
	PRIMARY KEY (sido_code, gugun_code),
	CONSTRAINT fk_guguns_sido
		FOREIGN KEY (sido_code)
		REFERENCES tourism_source.sidos (sido_code)
		ON DELETE CASCADE
);

CREATE INDEX idx_guguns_sido
	ON tourism_source.guguns (sido_code);
