CREATE TABLE trip.trip_invites (
  id uuid PRIMARY KEY,
  trip_id uuid NOT NULL,
  created_by_user_id uuid NOT NULL,
  invitee_user_id uuid,
  invite_code varchar(32) NOT NULL,
  invite_token_hash text NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  expires_at timestamptz,
  accepted_by_user_id uuid,
  accepted_at timestamptz,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL,
  CONSTRAINT trip_invites_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT trip_invites_invite_token_hash_unique UNIQUE (invite_token_hash),
  CONSTRAINT trip_invites_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED')),
  CONSTRAINT trip_invites_invite_code_length_check CHECK (char_length(invite_code) BETWEEN 4 AND 32)
);

CREATE INDEX trip_invites_trip_id_idx ON trip.trip_invites (trip_id);
CREATE INDEX trip_invites_invitee_user_id_idx ON trip.trip_invites (invitee_user_id);
CREATE INDEX trip_invites_invite_code_idx ON trip.trip_invites (invite_code);
CREATE INDEX trip_invites_status_idx ON trip.trip_invites (status);
