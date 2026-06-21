CREATE SCHEMA IF NOT EXISTS media;
CREATE SCHEMA IF NOT EXISTS record;

CREATE TABLE media.media_files (
  id uuid PRIMARY KEY,
  owner_user_id uuid,
  storage_provider varchar(40) NOT NULL DEFAULT 'S3_COMPATIBLE',
  bucket varchar(120) NOT NULL,
  object_key text NOT NULL,
  public_url text,
  mime_type varchar(120),
  byte_size bigint,
  width integer,
  height integer,
  linked_resource_type varchar(60),
  linked_resource_id uuid,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL,
  deleted_at timestamptz,
  purge_after_at timestamptz,
  purged_at timestamptz,
  CONSTRAINT media_files_status_check CHECK (status IN ('ACTIVE', 'DELETED', 'PURGED'))
);

CREATE INDEX media_files_owner_user_id_idx ON media.media_files (owner_user_id);
CREATE INDEX media_files_linked_resource_idx ON media.media_files (linked_resource_type, linked_resource_id);
CREATE INDEX media_files_status_idx ON media.media_files (status);

CREATE TABLE record.trip_record_entries (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  itinerary_day_id uuid,
  itinerary_item_id uuid,
  uploaded_by_user_id uuid NOT NULL,
  title varchar(160),
  caption text,
  location_name varchar(240),
  lat decimal(10,7),
  lng decimal(10,7),
  taken_at timestamptz,
  visibility varchar(30) NOT NULL DEFAULT 'TRIP_MEMBERS',
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT trip_record_entries_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT trip_record_entries_day_id_fk FOREIGN KEY (itinerary_day_id) REFERENCES itinerary.itinerary_days (id) ON DELETE SET NULL,
  CONSTRAINT trip_record_entries_item_id_fk FOREIGN KEY (itinerary_item_id) REFERENCES itinerary.itinerary_items (id) ON DELETE SET NULL,
  CONSTRAINT trip_record_entries_visibility_check CHECK (visibility IN ('TRIP_MEMBERS')),
  CONSTRAINT trip_record_entries_status_check CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE INDEX trip_record_entries_trip_id_idx ON record.trip_record_entries (trip_id);
CREATE INDEX trip_record_entries_itinerary_day_id_idx ON record.trip_record_entries (itinerary_day_id);
CREATE INDEX trip_record_entries_itinerary_item_id_idx ON record.trip_record_entries (itinerary_item_id);
CREATE INDEX trip_record_entries_uploaded_by_user_id_idx ON record.trip_record_entries (uploaded_by_user_id);
CREATE INDEX trip_record_entries_taken_at_idx ON record.trip_record_entries (taken_at);
CREATE INDEX trip_record_entries_status_idx ON record.trip_record_entries (status);
CREATE INDEX trip_record_entries_created_at_idx ON record.trip_record_entries (created_at);

CREATE TABLE record.trip_record_media (
  record_entry_id uuid NOT NULL,
  media_file_id uuid NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  caption text,
  created_at timestamptz NOT NULL,
  PRIMARY KEY (record_entry_id, media_file_id),
  CONSTRAINT trip_record_media_record_entry_id_fk FOREIGN KEY (record_entry_id) REFERENCES record.trip_record_entries (id) ON DELETE CASCADE,
  CONSTRAINT trip_record_media_media_file_id_fk FOREIGN KEY (media_file_id) REFERENCES media.media_files (id) ON DELETE CASCADE
);

CREATE INDEX trip_record_media_media_file_id_idx ON record.trip_record_media (media_file_id);
CREATE INDEX trip_record_media_record_sort_order_idx ON record.trip_record_media (record_entry_id, sort_order);
