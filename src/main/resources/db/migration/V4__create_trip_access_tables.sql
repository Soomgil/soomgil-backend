CREATE SCHEMA IF NOT EXISTS trip;

CREATE TABLE trip.trips (
  id uuid PRIMARY KEY,
  owner_user_id uuid NOT NULL,
  title varchar(160) NOT NULL,
  display_destination varchar(160),
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  itinerary_version bigint NOT NULL DEFAULT 0,
  retripped_from_post_id uuid,
  retripped_from_snapshot_version integer,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT trip_trips_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
  CONSTRAINT trip_trips_itinerary_version_check CHECK (itinerary_version >= 0)
);

CREATE INDEX trip_trips_owner_user_id_idx ON trip.trips (owner_user_id);
CREATE INDEX trip_trips_status_idx ON trip.trips (status);
CREATE INDEX trip_trips_retripped_from_post_id_idx ON trip.trips (retripped_from_post_id);
CREATE INDEX trip_trips_created_at_idx ON trip.trips (created_at);

CREATE TABLE trip.trip_regions (
  trip_id uuid NOT NULL,
  legal_region_code varchar(10) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  PRIMARY KEY (trip_id, legal_region_code),
  CONSTRAINT trip_regions_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT trip_regions_legal_region_code_length_check CHECK (char_length(legal_region_code) = 10)
);

CREATE INDEX trip_regions_legal_region_code_idx ON trip.trip_regions (legal_region_code);

CREATE TABLE trip.trip_members (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  user_id uuid NOT NULL,
  role varchar(20) NOT NULL DEFAULT 'MEMBER',
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  joined_at timestamptz NOT NULL,
  left_at timestamptz,
  removed_by_user_id uuid,
  CONSTRAINT trip_members_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT trip_members_trip_user_unique UNIQUE (trip_id, user_id),
  CONSTRAINT trip_members_role_check CHECK (role IN ('MEMBER')),
  CONSTRAINT trip_members_status_check CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED'))
);

CREATE INDEX trip_members_user_id_idx ON trip.trip_members (user_id);
CREATE INDEX trip_members_trip_id_role_idx ON trip.trip_members (trip_id, role);
CREATE INDEX trip_members_status_idx ON trip.trip_members (status);
