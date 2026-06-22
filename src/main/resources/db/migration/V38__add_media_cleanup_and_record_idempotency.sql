CREATE TABLE media.upload_intents (
  id uuid PRIMARY KEY,
  owner_user_id uuid NOT NULL,
  object_key text NOT NULL UNIQUE,
  status varchar(20) NOT NULL,
  media_file_id uuid,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL,
  completed_at timestamptz,
  CONSTRAINT media_upload_intents_status_check CHECK (status IN ('PENDING', 'PURGING', 'COMPLETED', 'PURGED')),
  CONSTRAINT media_upload_intents_media_file_fk FOREIGN KEY (media_file_id) REFERENCES media.media_files (id) ON DELETE SET NULL
);

CREATE INDEX media_upload_intents_cleanup_idx ON media.upload_intents (status, expires_at);

CREATE TABLE record.trip_record_create_requests (
  user_id uuid NOT NULL,
  trip_id uuid NOT NULL,
  idempotency_key varchar(128) NOT NULL,
  request_hash varchar(64) NOT NULL,
  record_id uuid NOT NULL,
  created_at timestamptz NOT NULL,
  PRIMARY KEY (user_id, trip_id, idempotency_key),
  CONSTRAINT trip_record_create_requests_record_fk
    FOREIGN KEY (record_id) REFERENCES record.trip_record_entries (id) ON DELETE CASCADE
);
