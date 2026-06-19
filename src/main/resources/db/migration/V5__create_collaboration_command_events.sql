CREATE SCHEMA IF NOT EXISTS collab;

CREATE TABLE collab.collaboration_command_events (
  id bigserial PRIMARY KEY,
  trip_id uuid NOT NULL,
  actor_user_id uuid NOT NULL,
  websocket_session_id varchar(120),
  source varchar(30) NOT NULL,
  command_type varchar(80) NOT NULL,
  aggregate_type varchar(80) NOT NULL,
  aggregate_id uuid,
  version_before bigint,
  version_after bigint,
  payload jsonb NOT NULL,
  inverse_payload jsonb,
  redo_payload jsonb,
  created_at timestamptz NOT NULL,
  CONSTRAINT collaboration_command_events_trip_id_fk FOREIGN KEY (trip_id) REFERENCES trip.trips (id) ON DELETE CASCADE,
  CONSTRAINT collaboration_command_events_source_check CHECK (source IN ('USER', 'AI_TOOL', 'UNDO', 'REDO'))
);

CREATE INDEX collaboration_command_events_trip_id_idx ON collab.collaboration_command_events (trip_id);
CREATE INDEX collaboration_command_events_actor_user_id_idx ON collab.collaboration_command_events (actor_user_id);
CREATE INDEX collaboration_command_events_websocket_session_id_idx
  ON collab.collaboration_command_events (websocket_session_id);
CREATE INDEX collaboration_command_events_command_type_idx ON collab.collaboration_command_events (command_type);
CREATE INDEX collaboration_command_events_created_at_idx ON collab.collaboration_command_events (created_at);
