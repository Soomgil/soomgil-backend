-- ============================================================
-- 00_run_all.sql
-- Master runner — executes all seed files in FK-safe order.
--
-- Usage:
--   psql -d soomgil -f 00_run_all.sql
--
-- Or from psql prompt:
--   \i 00_run_all.sql
--
-- Each file is wrapped in BEGIN/COMMIT and uses ON CONFLICT DO NOTHING,
-- so re-running is safe (idempotent).
--
-- UUID conventions (all deterministic for cross-file references):
--   Users:             a0000000-0000-4000-8000-0000000000NN  (20 users)
--   Media (avatars):   b0000000-0000-4000-8000-0000000000NN
--   Media (trips):     b1000000-0000-4000-8000-0000000000NN
--   Trips:             c0000000-0000-4000-8000-0000000000NN  (22 trips)
--   Itinerary days:    dNNNN0000-0000-4000-8000-0000000000NN
--   Itinerary items:   eNNNNNN00-0000-4000-8000-0000000000NN
--   Trip routes:       f2000000-0000-4000-8000-0000000000NN
--   Map drawings:      4d000000-0000-4000-8000-0000000000NN
--   Trip notes:        1e000000-0000-4000-8000-0000000000NN
--   Checklists:        2e000000-0000-4000-8000-0000000000NN
--   Checklist items:   3e000000-0000-4000-8000-0000000000NN
--   Chat messages:     5e000000-0000-4000-8000-0000000000NN
--   Place enrichments: 6e000000-0000-4000-8000-0000000000NN
--   Place reactions:   7e000000-0000-4000-8000-0000000000NN
--   Saved places:      8e000000-0000-4000-8000-0000000000NN
--   AI sessions:       9e000000-0000-4000-8000-0000000000NN
--   AI messages:       a1000000-0000-4000-8000-0000000000NN
--   AI tool calls:     b1000000-0000-4000-8000-0000000000NN
--   Record entries:    c1e00000-0000-4000-8000-0000000000NN
--   Posts:             10000000-0000-4000-8000-0000000000NN  (16 posts)
--   Post snapshot days:    11000000-0000-4000-8000-0000000000NN
--   Post snapshot items:   12000000-0000-4000-8000-0000000000NN
--   Post snapshot routes:  13000000-0000-4000-8000-0000000000NN
--   Post retrips:          14000000-0000-4000-8000-0000000000NN
--   Post comments:         15000000-0000-4000-8000-0000000000NN
--   Content reports:       16000000-0000-4000-8000-0000000000NN
--   Notifications:         17000000-0000-4000-8000-0000000000NN
--   Hashtags:              30000000-0000-4000-8000-0000000000NN
--   Preference tags:       2b000000-0000-4000-8000-0000000000NN
-- ============================================================

\echo '→ Loading 01_auth.sql ...'
\i 01_auth.sql

\echo '→ Loading 02_social_media.sql ...'
\i 02_social_media.sql

\echo '→ Loading 03_geo.sql ...'
\i 03_geo.sql

\echo '→ Loading 04_trips_members.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM trip.trips WHERE id = 'c0000000-0000-4000-8000-000000000001'
) AS load_trip_seed \gset
\if :load_trip_seed
  \i 04_trips_members.sql
\else
  \echo '  ↳ demo trips already exist; skipping 04'
\endif

\echo '→ Loading 05_itinerary.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM itinerary.itinerary_days WHERE id = 'd0101000-0000-4000-8000-000000000001'
) AS load_itinerary_seed \gset
\if :load_itinerary_seed
  \i 05_itinerary.sql
\else
  \echo '  ↳ demo itinerary already exists; skipping 05'
\endif

\echo '→ Loading 06_planning_chat.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM planning.trip_notes WHERE id = '1e000000-0000-4000-8000-000000000001'
) AS load_planning_seed \gset
\if :load_planning_seed
  \i 06_planning_chat.sql
\else
  \echo '  ↳ demo planning/chat already exists; skipping 06'
\endif

\echo '→ Skipping legacy 07_preference.sql (official tag dictionary is seeded by Flyway V28)'

\echo '→ Loading 08_ai.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM ai.ai_chat_sessions WHERE id = '9e000000-0000-4000-8000-000000000001'
) AS load_ai_seed \gset
\if :load_ai_seed
  \i 08_ai.sql
\else
  \echo '  ↳ demo AI data already exists; skipping 08'
\endif

\echo '→ Loading 09_record.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM record.trip_record_entries WHERE id = 'c1e00000-0000-4000-8000-000000000001'
) AS load_record_seed \gset
\if :load_record_seed
  \i 09_record.sql
\else
  \echo '  ↳ demo records already exist; skipping 09'
\endif

\echo '→ Loading 10_community.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM community.posts WHERE id = '10000000-0000-4000-8000-000000000001'
) AS load_community_seed \gset
\if :load_community_seed
  \i 10_community.sql
\else
  \echo '  ↳ demo community data already exists; skipping 10'
\endif

\echo '→ Loading 11_notification_collab_ops.sql ...'
SELECT NOT EXISTS (
  SELECT 1 FROM notification.notifications WHERE id = '17000000-0000-4000-8000-000000000001'
) AS load_notification_seed \gset
\if :load_notification_seed
  \i 11_notification_collab_ops.sql
\else
  \echo '  ↳ demo notifications already exist; skipping 11'
\endif

\echo '→ Loading 12_local_user_enrichment.sql ...'
\i 12_local_user_enrichment.sql

\echo '✓ All dummy data loaded.'
