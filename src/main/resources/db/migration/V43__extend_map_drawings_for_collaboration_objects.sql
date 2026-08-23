ALTER TABLE itinerary.map_drawings
  ADD COLUMN media_file_id uuid,
  ADD COLUMN sticker_code varchar(80),
  ADD COLUMN transform jsonb;

ALTER TABLE itinerary.map_drawings
  DROP CONSTRAINT map_drawings_drawing_type_check,
  ADD CONSTRAINT map_drawings_drawing_type_check
    CHECK (drawing_type IN ('FREEHAND', 'LINE', 'POLYGON', 'MARKER', 'TEXT', 'STICKER', 'IMAGE')),
  ADD CONSTRAINT map_drawings_media_file_id_fk
    FOREIGN KEY (media_file_id) REFERENCES media.media_files (id) ON DELETE RESTRICT,
  ADD CONSTRAINT map_drawings_object_fields_check CHECK (
    (drawing_type = 'STICKER' AND sticker_code IS NOT NULL AND media_file_id IS NULL AND transform IS NOT NULL)
    OR (drawing_type = 'IMAGE' AND media_file_id IS NOT NULL AND sticker_code IS NULL AND transform IS NOT NULL)
    OR (drawing_type NOT IN ('STICKER', 'IMAGE') AND media_file_id IS NULL AND sticker_code IS NULL AND transform IS NULL)
  );

CREATE INDEX map_drawings_media_file_id_idx ON itinerary.map_drawings (media_file_id);
