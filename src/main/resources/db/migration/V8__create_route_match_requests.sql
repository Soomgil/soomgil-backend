CREATE TABLE itinerary.route_match_requests (
  id BIGSERIAL PRIMARY KEY,
  trip_id UUID NOT NULL,
  trip_route_id UUID,
  origin_itinerary_item_id UUID NOT NULL,
  destination_itinerary_item_id UUID NOT NULL,
  requested_by_user_id UUID NOT NULL,
  provider VARCHAR(40) NOT NULL DEFAULT 'MAPBOX',
  provider_profile VARCHAR(80) NOT NULL,
  input_coordinates JSONB NOT NULL,
  radiuses JSONB,
  tidy BOOLEAN NOT NULL DEFAULT FALSE,
  request_hash VARCHAR(128),
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  confidence DOUBLE PRECISION,
  distance_meters DOUBLE PRECISION,
  duration_seconds DOUBLE PRECISION,
  tracepoints JSONB,
  matchings_metadata JSONB,
  error_code VARCHAR(80),
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  CONSTRAINT route_match_requests_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT route_match_requests_trip_route_id_fk FOREIGN KEY (trip_route_id) REFERENCES itinerary.trip_routes (id) ON DELETE SET NULL,
  CONSTRAINT route_match_requests_origin_item_id_fk FOREIGN KEY (origin_itinerary_item_id) REFERENCES itinerary.itinerary_items (id) ON DELETE CASCADE,
  CONSTRAINT route_match_requests_destination_item_id_fk FOREIGN KEY (destination_itinerary_item_id) REFERENCES itinerary.itinerary_items (id) ON DELETE CASCADE,
  CONSTRAINT route_match_requests_status_check CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT route_match_requests_provider_check CHECK (provider IN ('MAPBOX')),
  CONSTRAINT route_match_requests_distinct_items_check CHECK (origin_itinerary_item_id <> destination_itinerary_item_id)
);

CREATE INDEX route_match_requests_trip_id_idx ON itinerary.route_match_requests (trip_id);
CREATE INDEX route_match_requests_trip_route_id_idx ON itinerary.route_match_requests (trip_route_id);
CREATE INDEX route_match_requests_requested_by_user_id_idx ON itinerary.route_match_requests (requested_by_user_id);
CREATE INDEX route_match_requests_status_idx ON itinerary.route_match_requests (status);
CREATE INDEX route_match_requests_request_hash_idx ON itinerary.route_match_requests (request_hash);
