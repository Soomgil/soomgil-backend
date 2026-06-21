DO $$
DECLARE target record;
BEGIN
  IF to_regclass('public._dev_seed_added_defaults') IS NULL THEN
    RETURN;
  END IF;

  FOR target IN SELECT * FROM public._dev_seed_added_defaults LOOP
    EXECUTE format(
      'ALTER TABLE %I.%I ALTER COLUMN %I DROP DEFAULT',
      target.table_schema, target.table_name, target.column_name
    );
  END LOOP;
END $$;

DROP TABLE IF EXISTS public._dev_seed_added_defaults;
