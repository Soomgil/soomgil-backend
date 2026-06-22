-- Connect real locally registered users to deterministic demo data.
-- Seed-only users use the a0000000 prefix and are intentionally excluded.
BEGIN;

WITH local_users AS (
  SELECT id
  FROM auth.users
  WHERE id::text NOT LIKE 'a0000000-%'
), demo_trips AS (
  SELECT id
  FROM trip.trips
  WHERE id::text LIKE 'c0000000-%'
  ORDER BY id
  LIMIT 8
)
INSERT INTO trip.trip_members (id, trip_id, user_id, role, status)
SELECT gen_random_uuid(), trip.id, local_user.id, 'MEMBER', 'ACTIVE'
FROM local_users local_user
CROSS JOIN demo_trips trip
WHERE NOT EXISTS (
  SELECT 1
  FROM trip.trip_members member
  WHERE member.trip_id = trip.id
    AND member.user_id = local_user.id
)
ON CONFLICT DO NOTHING;

WITH local_users AS (
  SELECT id
  FROM auth.users
  WHERE id::text NOT LIKE 'a0000000-%'
), demo_users AS (
  SELECT id
  FROM auth.users
  WHERE id::text LIKE 'a0000000-%'
  ORDER BY id
  LIMIT 10
)
INSERT INTO social.user_follows (follower_user_id, following_user_id, status)
SELECT local_user.id, demo_user.id, 'ACTIVE'
FROM local_users local_user
CROSS JOIN demo_users demo_user
UNION ALL
SELECT demo_user.id, local_user.id, 'ACTIVE'
FROM local_users local_user
CROSS JOIN demo_users demo_user
ON CONFLICT (follower_user_id, following_user_id) DO NOTHING;

COMMIT;
