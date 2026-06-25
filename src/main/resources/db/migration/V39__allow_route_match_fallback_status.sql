ALTER TABLE itinerary.route_match_requests
  DROP CONSTRAINT IF EXISTS route_match_requests_status_check;

ALTER TABLE itinerary.route_match_requests
  ADD CONSTRAINT route_match_requests_status_check
  CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'FALLBACK'));
