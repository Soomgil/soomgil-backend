CREATE SCHEMA IF NOT EXISTS itinerary;

CREATE TABLE itinerary.itinerary_days (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  group_type varchar(30) NOT NULL DEFAULT 'DAY',
  day_number integer,
  date date,
  title varchar(120),
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT itinerary_days_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT itinerary_days_group_type_check CHECK (group_type IN ('DAY', 'UNSCHEDULED')),
  CONSTRAINT itinerary_days_day_number_check CHECK (
    (group_type = 'DAY' AND day_number IS NOT NULL)
    OR (group_type = 'UNSCHEDULED' AND day_number IS NULL)
  )
);

CREATE UNIQUE INDEX itinerary_days_trip_group_day_unique
  ON itinerary.itinerary_days (trip_id, group_type, day_number)
  NULLS NOT DISTINCT;
CREATE INDEX itinerary_days_trip_id_idx ON itinerary.itinerary_days (trip_id);
CREATE INDEX itinerary_days_trip_sort_order_idx ON itinerary.itinerary_days (trip_id, sort_order);

CREATE TABLE itinerary.itinerary_items (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  itinerary_day_id uuid NOT NULL,
  sort_order integer NOT NULL,
  item_type varchar(30) NOT NULL DEFAULT 'PLACE',
  place_provider varchar(40),
  external_place_id varchar(120),
  place_name varchar(240) NOT NULL,
  address text,
  lat decimal(10,7),
  lng decimal(10,7),
  thumbnail_url text,
  source_status varchar(30) NOT NULL DEFAULT 'AVAILABLE',
  created_by_user_id uuid,
  updated_by_user_id uuid,
  deleted_by_user_id uuid,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT itinerary_items_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT itinerary_items_day_id_fk FOREIGN KEY (itinerary_day_id) REFERENCES itinerary.itinerary_days (id) ON DELETE CASCADE,
  CONSTRAINT itinerary_items_item_type_check CHECK (item_type IN ('PLACE', 'CUSTOM_PLACE')),
  CONSTRAINT itinerary_items_source_status_check CHECK (source_status IN ('AVAILABLE', 'DELETED', 'UNKNOWN')),
  CONSTRAINT itinerary_items_place_ref_check CHECK (
    (item_type = 'PLACE' AND place_provider IS NOT NULL AND external_place_id IS NOT NULL)
    OR item_type = 'CUSTOM_PLACE'
  )
);

CREATE INDEX itinerary_items_trip_id_idx ON itinerary.itinerary_items (trip_id);
CREATE INDEX itinerary_items_day_id_idx ON itinerary.itinerary_items (itinerary_day_id);
CREATE INDEX itinerary_items_day_sort_order_idx ON itinerary.itinerary_items (itinerary_day_id, sort_order);
CREATE INDEX itinerary_items_place_ref_idx ON itinerary.itinerary_items (place_provider, external_place_id);
CREATE INDEX itinerary_items_deleted_at_idx ON itinerary.itinerary_items (deleted_at);
