-- ============================================================
-- 02_social_media.sql
-- social.user_follows + media.media_files
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- media.media_files
-- Profile avatars + trip photos + community post covers
-- Pattern: b0000000-0000-4000-8000-00000000000N
-- ------------------------------------------------------------

-- Profile avatars (linked_resource_type=auth.users)
INSERT INTO media.media_files (id, owner_user_id, storage_provider, bucket, object_key, public_url, mime_type, byte_size, width, height, linked_resource_type, linked_resource_id, status, created_at) VALUES
  ('b0000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-000000000001','S3_COMPATIBLE','soomgil-media','avatar/u01.webp','https://cdn.soomgil.test/avatar/u01.webp','image/webp', 86000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000001', 'ACTIVE', now()),
  ('b0000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000002','S3_COMPATIBLE','soomgil-media','avatar/u02.webp','https://cdn.soomgil.test/avatar/u02.webp','image/webp', 91000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000002', 'ACTIVE', now()),
  ('b0000000-0000-4000-8000-000000000003','a0000000-0000-4000-8000-000000000003','S3_COMPATIBLE','soomgil-media','avatar/u03.webp','https://cdn.soomgil.test/avatar/u03.webp','image/webp', 78000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000003', 'ACTIVE', now()),
  ('b0000000-0000-4000-8000-000000000004','a0000000-0000-4000-8000-000000000004','S3_COMPATIBLE','soomgil-media','avatar/u04.webp','https://cdn.soomgil.test/avatar/u04.webp','image/webp', 82000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000004', 'ACTIVE', now()),
  ('b0000000-0000-4000-8000-000000000005','a0000000-0000-4000-8000-000000000005','S3_COMPATIBLE','soomgil-media','avatar/u05.webp','https://cdn.soomgil.test/avatar/u05.webp','image/webp', 88000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000005', 'ACTIVE', now()),
  ('b0000000-0000-4000-8000-000000000010','a0000000-0000-4000-8000-000000000010','S3_COMPATIBLE','soomgil-media','avatar/u10.webp','https://cdn.soomgil.test/avatar/u10.webp','image/webp', 79000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000010', 'ACTIVE', now()),
  ('b0000000-0000-4000-8000-000000000015','a0000000-0000-4000-8000-000000000015','S3_COMPATIBLE','soomgil-media','avatar/u15.webp','https://cdn.soomgil.test/avatar/u15.webp','image/webp', 94000, 256, 256, 'auth.users', 'a0000000-0000-4000-8000-000000000015', 'ACTIVE', now())
ON CONFLICT (id) DO NOTHING;

-- Update user_profiles.profile_media_file_id to point at avatars
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000001' WHERE user_id = 'a0000000-0000-4000-8000-000000000001';
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000002' WHERE user_id = 'a0000000-0000-4000-8000-000000000002';
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000003' WHERE user_id = 'a0000000-0000-4000-8000-000000000003';
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000004' WHERE user_id = 'a0000000-0000-4000-8000-000000000004';
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000005' WHERE user_id = 'a0000000-0000-4000-8000-000000000005';
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000010' WHERE user_id = 'a0000000-0000-4000-8000-000000000010';
UPDATE auth.user_profiles SET profile_media_file_id = 'b0000000-0000-4000-8000-000000000015' WHERE user_id = 'a0000000-0000-4000-8000-000000000015';

-- Trip photos (linked_resource_type=trip.trips) — will be used by community posts / records
INSERT INTO media.media_files (id, owner_user_id, storage_provider, bucket, object_key, public_url, mime_type, byte_size, width, height, linked_resource_type, linked_resource_id, status, created_at) VALUES
  ('b1000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-000000000002','S3_COMPATIBLE','soomgil-media','trip/c01/photo01.jpg','https://cdn.soomgil.test/trip/c01/photo01.jpg','image/jpeg',480000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000001','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000002','S3_COMPATIBLE','soomgil-media','trip/c01/photo02.jpg','https://cdn.soomgil.test/trip/c01/photo02.jpg','image/jpeg',520000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000001','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000003','a0000000-0000-4000-8000-000000000002','S3_COMPATIBLE','soomgil-media','trip/c01/photo03.jpg','https://cdn.soomgil.test/trip/c01/photo03.jpg','image/jpeg',440000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000001','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000010','a0000000-0000-4000-8000-000000000003','S3_COMPATIBLE','soomgil-media','trip/c02/photo01.jpg','https://cdn.soomgil.test/trip/c02/photo01.jpg','image/jpeg',610000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000002','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000020','a0000000-0000-4000-8000-000000000004','S3_COMPATIBLE','soomgil-media','trip/c03/photo01.jpg','https://cdn.soomgil.test/trip/c03/photo01.jpg','image/jpeg',580000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000003','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000030','a0000000-0000-4000-8000-000000000005','S3_COMPATIBLE','soomgil-media','trip/c04/photo01.jpg','https://cdn.soomgil.test/trip/c04/photo01.jpg','image/jpeg',500000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000004','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000040','a0000000-0000-4000-8000-000000000006','S3_COMPATIBLE','soomgil-media','trip/c05/photo01.jpg','https://cdn.soomgil.test/trip/c05/photo01.jpg','image/jpeg',530000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000005','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000050','a0000000-0000-4000-8000-000000000007','S3_COMPATIBLE','soomgil-media','trip/c06/photo01.jpg','https://cdn.soomgil.test/trip/c06/photo01.jpg','image/jpeg',470000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000006','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000060','a0000000-0000-4000-8000-000000000008','S3_COMPATIBLE','soomgil-media','trip/c07/photo01.jpg','https://cdn.soomgil.test/trip/c07/photo01.jpg','image/jpeg',490000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000007','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000070','a0000000-0000-4000-8000-000000000009','S3_COMPATIBLE','soomgil-media','trip/c08/photo01.jpg','https://cdn.soomgil.test/trip/c08/photo01.jpg','image/jpeg',450000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000008','ACTIVE',now()),
  ('b1000000-0000-4000-8000-000000000080','a0000000-0000-4000-8000-000000000010','S3_COMPATIBLE','soomgil-media','trip/c09/photo01.jpg','https://cdn.soomgil.test/trip/c09/photo01.jpg','image/jpeg',460000,1920,1280,'trip.trips','c0000000-0000-4000-8000-000000000009','ACTIVE',now())
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- social.user_follows
-- Organic follow graph for user discovery / search
-- ------------------------------------------------------------
INSERT INTO social.user_follows (follower_user_id, following_user_id, status) VALUES
  ('a0000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000001','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000003','a0000000-0000-4000-8000-000000000001','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000004','a0000000-0000-4000-8000-000000000002','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000005','a0000000-0000-4000-8000-000000000002','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000006','a0000000-0000-4000-8000-000000000002','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000007','a0000000-0000-4000-8000-000000000003','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000008','a0000000-0000-4000-8000-000000000003','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000009','a0000000-0000-4000-8000-000000000004','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000010','a0000000-0000-4000-8000-000000000004','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000011','a0000000-0000-4000-8000-000000000005','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000012','a0000000-0000-4000-8000-000000000005','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000013','a0000000-0000-4000-8000-000000000006','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000014','a0000000-0000-4000-8000-000000000006','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000015','a0000000-0000-4000-8000-000000000007','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-000000000008','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000009','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000003','a0000000-0000-4000-8000-000000000010','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000004','a0000000-0000-4000-8000-000000000011','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000005','a0000000-0000-4000-8000-000000000012','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000017','a0000000-0000-4000-8000-000000000013','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000018','a0000000-0000-4000-8000-000000000014','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000019','a0000000-0000-4000-8000-000000000015','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000020','a0000000-0000-4000-8000-000000000001','ACTIVE'),
  ('a0000000-0000-4000-8000-000000000016','a0000000-0000-4000-8000-000000000002','DELETED')
ON CONFLICT (follower_user_id, following_user_id) DO NOTHING;

COMMIT;
