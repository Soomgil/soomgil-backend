CREATE TABLE itinerary.trip_routes (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  origin_itinerary_item_id uuid NOT NULL,
  destination_itinerary_item_id uuid NOT NULL,
  mode varchar(20) NOT NULL,
  provider varchar(40) NOT NULL DEFAULT 'MAPBOX',
  provider_profile varchar(80) NOT NULL,
  geometry_format varchar(30) NOT NULL DEFAULT 'GEOJSON',
  geometry jsonb NOT NULL,
  distance_meters decimal(12,2),
  duration_seconds decimal(12,2),
  confidence decimal(5,4),
  created_by_user_id uuid,
  updated_by_user_id uuid,
  deleted_by_user_id uuid,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT trip_routes_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT trip_routes_origin_item_id_fk FOREIGN KEY (origin_itinerary_item_id) REFERENCES itinerary.itinerary_items (id) ON DELETE CASCADE,
  CONSTRAINT trip_routes_destination_item_id_fk FOREIGN KEY (destination_itinerary_item_id) REFERENCES itinerary.itinerary_items (id) ON DELETE CASCADE,
  CONSTRAINT trip_routes_mode_check CHECK (mode IN ('DRIVING', 'WALKING')),
  CONSTRAINT trip_routes_geometry_format_check CHECK (geometry_format IN ('GEOJSON')),
  CONSTRAINT trip_routes_distinct_items_check CHECK (origin_itinerary_item_id <> destination_itinerary_item_id)
);

CREATE INDEX trip_routes_trip_id_idx ON itinerary.trip_routes (trip_id);
CREATE INDEX trip_routes_origin_itinerary_item_id_idx ON itinerary.trip_routes (origin_itinerary_item_id);
CREATE INDEX trip_routes_destination_itinerary_item_id_idx ON itinerary.trip_routes (destination_itinerary_item_id);
CREATE INDEX trip_routes_origin_destination_idx
  ON itinerary.trip_routes (origin_itinerary_item_id, destination_itinerary_item_id);
CREATE INDEX trip_routes_deleted_at_idx ON itinerary.trip_routes (deleted_at);
