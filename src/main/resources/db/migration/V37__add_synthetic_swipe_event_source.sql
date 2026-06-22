ALTER TABLE preference.synthetic_swipe_events
	ADD COLUMN source varchar(30) NOT NULL DEFAULT 'SYNTHETIC_PERSONA';

ALTER TABLE preference.synthetic_swipe_events
	ADD CONSTRAINT chk_synthetic_swipe_events_source
	CHECK (source = 'SYNTHETIC_PERSONA');
