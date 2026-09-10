ALTER TABLE planning.trip_notes
  ADD COLUMN version bigint NOT NULL DEFAULT 1;
