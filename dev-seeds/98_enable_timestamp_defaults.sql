CREATE TABLE IF NOT EXISTS public._dev_seed_added_defaults (
  table_schema text NOT NULL,
  table_name text NOT NULL,
  column_name text NOT NULL,
  PRIMARY KEY (table_schema, table_name, column_name)
);

TRUNCATE public._dev_seed_added_defaults;

INSERT INTO public._dev_seed_added_defaults (table_schema, table_name, column_name)
SELECT table_schema, table_name, column_name
FROM information_schema.columns
WHERE table_schema IN (
  'auth', 'social', 'media', 'geo', 'trip', 'itinerary', 'planning',
  'chat', 'preference', 'ai', 'record', 'community', 'notification',
  'collab', 'ops'
)
  AND data_type IN ('timestamp with time zone', 'timestamp without time zone')
  AND is_nullable = 'NO'
  AND column_default IS NULL;

DO $$
DECLARE target record;
BEGIN
  FOR target IN SELECT * FROM public._dev_seed_added_defaults LOOP
    EXECUTE format(
      'ALTER TABLE %I.%I ALTER COLUMN %I SET DEFAULT now()',
      target.table_schema, target.table_name, target.column_name
    );
  END LOOP;
END $$;
