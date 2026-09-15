ALTER TABLE itinerary.trip_routes DROP CONSTRAINT trip_routes_mode_check;
ALTER TABLE itinerary.trip_routes ADD CONSTRAINT trip_routes_mode_check
  CHECK (mode IN ('DRIVING', 'WALKING', 'CYCLING'));
