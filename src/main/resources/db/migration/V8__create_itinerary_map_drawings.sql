CREATE TABLE itinerary.map_drawings (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  itinerary_day_id uuid,
  drawing_type varchar(30) NOT NULL,
  geometry_format varchar(30) NOT NULL DEFAULT 'GEOJSON',
  geometry jsonb NOT NULL,
  style jsonb,
  label varchar(160),
  sort_order integer NOT NULL DEFAULT 0,
  version bigint NOT NULL DEFAULT 0,
  created_by_user_id uuid NOT NULL,
  updated_by_user_id uuid,
  deleted_by_user_id uuid,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT map_drawings_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT map_drawings_day_id_fk FOREIGN KEY (itinerary_day_id) REFERENCES itinerary.itinerary_days (id) ON DELETE SET NULL,
  CONSTRAINT map_drawings_drawing_type_check CHECK (drawing_type IN ('FREEHAND', 'LINE', 'POLYGON', 'MARKER', 'TEXT')),
  CONSTRAINT map_drawings_geometry_format_check CHECK (geometry_format IN ('GEOJSON')),
  CONSTRAINT map_drawings_version_check CHECK (version >= 0)
);

CREATE INDEX map_drawings_trip_id_idx ON itinerary.map_drawings (trip_id);
CREATE INDEX map_drawings_itinerary_day_id_idx ON itinerary.map_drawings (itinerary_day_id);
CREATE INDEX map_drawings_created_by_user_id_idx ON itinerary.map_drawings (created_by_user_id);
CREATE INDEX map_drawings_drawing_type_idx ON itinerary.map_drawings (drawing_type);
CREATE INDEX map_drawings_trip_sort_order_idx ON itinerary.map_drawings (trip_id, sort_order);
CREATE INDEX map_drawings_deleted_at_idx ON itinerary.map_drawings (deleted_at);
