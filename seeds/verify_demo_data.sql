\set ON_ERROR_STOP on

DO $$
DECLARE
  profile_count integer;
  distinct_bio_count integer;
  post_count integer;
  distinct_title_count integer;
  distinct_summary_count integer;
  comment_count integer;
  distinct_comment_count integer;
  reply_count integer;
  like_count integer;
  distinct_like_counts integer;
  stale_url_count integer;
  empty_demo_trip_count integer;
  missing_demo_thumbnail_count integer;
  missing_record_photo_count integer;
  portrait_record_photo_count integer;
  demo_active_trip_count integer;
  demo_archived_trip_count integer;
  demo_empty_trip_count integer;
  demo_empty_day_group_count integer;
  demo_incomplete_unscheduled_count integer;
  oversized_demo_trip_count integer;
  demo_authored_post_count integer;
  missing_post_cover_count integer;
BEGIN
  SELECT count(*), count(DISTINCT bio)
  INTO profile_count, distinct_bio_count
  FROM auth.user_profiles
  WHERE user_id IN (SELECT md5('demo-user:' || n)::uuid FROM generate_series(1, 120) n);

  WITH demo_posts AS (
    SELECT md5('demo-bulk-post:' || g)::uuid id FROM generate_series(1, 50) g
    UNION ALL
    SELECT md5('demo-post:' || k)::uuid FROM (VALUES
      ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
      ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')
    ) v(k)
  )
  SELECT count(*), count(DISTINCT title), count(DISTINCT summary)
  INTO post_count, distinct_title_count, distinct_summary_count
  FROM community.posts p JOIN demo_posts d ON d.id = p.id;

  WITH demo_posts AS (
    SELECT md5('demo-bulk-post:' || g)::uuid id FROM generate_series(1, 50) g
    UNION ALL
    SELECT md5('demo-post:' || k)::uuid FROM (VALUES
      ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
      ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')
    ) v(k)
  )
  SELECT count(*), count(DISTINCT content),
         count(*) FILTER (WHERE parent_comment_id IS NOT NULL AND depth = 1)
  INTO comment_count, distinct_comment_count, reply_count
  FROM community.post_comments c JOIN demo_posts d ON d.id = c.post_id;

  WITH demo_posts AS (
    SELECT md5('demo-bulk-post:' || g)::uuid id FROM generate_series(1, 50) g
    UNION ALL
    SELECT md5('demo-post:' || k)::uuid FROM (VALUES
      ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
      ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')
    ) v(k)
  ), counts AS (
    SELECT d.id, count(l.user_id) likes
    FROM demo_posts d LEFT JOIN community.post_likes l ON l.post_id = d.id
    GROUP BY d.id
  )
  SELECT sum(likes), count(DISTINCT likes)
  INTO like_count, distinct_like_counts
  FROM counts;

  SELECT count(*) INTO stale_url_count FROM (
    SELECT profile_image_url url
    FROM auth.user_profiles
    WHERE user_id IN (SELECT md5('demo-user:' || n)::uuid FROM generate_series(1, 120) n)
       OR user_id::text LIKE 'a0000000-%'
    UNION ALL
    SELECT public_url FROM media.media_files
    WHERE object_key LIKE 'demo/%'
       OR id::text LIKE 'b0000000-%'
       OR id::text LIKE 'b1000000-%'
    UNION ALL
    SELECT first_image1 FROM tourism_source.attractions
    WHERE content_id BETWEEN 10001 AND 10040 OR content_id BETWEEN 20001 AND 20028
    UNION ALL
    SELECT thumbnail_url FROM itinerary.itinerary_items
    UNION ALL
    SELECT thumbnail_url FROM community.post_snapshot_items
  ) urls
  WHERE url LIKE 'https://cdn.soomgil.test/%';

  SELECT count(*) INTO empty_demo_trip_count
  FROM trip.trips t
  WHERE t.id::text LIKE 'c0000000-%'
    AND t.deleted_at IS NULL
    AND NOT EXISTS (
      SELECT 1 FROM itinerary.itinerary_items i
      WHERE i.trip_id = t.id AND i.deleted_at IS NULL
    );

  SELECT count(*) INTO missing_record_photo_count
  FROM record.trip_record_media rm
  JOIN record.trip_record_entries r ON r.id = rm.record_entry_id
  JOIN media.media_files m ON m.id = rm.media_file_id
  WHERE r.status = 'ACTIVE'
    AND m.object_key LIKE 'demo/%'
    AND (m.public_url IS NULL OR m.public_url LIKE 'https://cdn.soomgil.test/%');

  SELECT count(*) INTO portrait_record_photo_count
  FROM record.trip_record_media rm
  JOIN record.trip_record_entries r ON r.id = rm.record_entry_id
  JOIN media.media_files m ON m.id = rm.media_file_id
  JOIN auth.user_email_addresses e ON e.user_id = r.uploaded_by_user_id
  WHERE e.normalized_email = 'demo01@soomgil.local'
    AND m.object_key LIKE 'demo/records/%/portrait%.jpg'
    AND m.height > m.width
    AND m.status = 'ACTIVE';

  WITH demo_user AS (
    SELECT user_id FROM auth.user_email_addresses
    WHERE normalized_email = 'demo01@soomgil.local'
  )
  SELECT count(*) FILTER (WHERE t.status = 'ACTIVE'),
         count(*) FILTER (WHERE t.status = 'ARCHIVED'),
         count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1 FROM itinerary.itinerary_items i
           WHERE i.trip_id = t.id AND i.deleted_at IS NULL
         ))
  INTO demo_active_trip_count, demo_archived_trip_count, demo_empty_trip_count
  FROM trip.trips t
  JOIN trip.trip_members tm ON tm.trip_id = t.id AND tm.status = 'ACTIVE'
  JOIN demo_user du ON du.user_id = tm.user_id
  WHERE t.status != 'DELETED';

  WITH demo_user AS (
    SELECT user_id FROM auth.user_email_addresses
    WHERE normalized_email = 'demo01@soomgil.local'
  ), visible_trips AS (
    SELECT t.id
    FROM trip.trips t
    JOIN trip.trip_members tm ON tm.trip_id = t.id AND tm.status = 'ACTIVE'
    JOIN demo_user du ON du.user_id = tm.user_id
    WHERE t.status != 'DELETED'
  )
  SELECT count(*) FILTER (WHERE item_count = 0),
         count(*) FILTER (WHERE group_type = 'UNSCHEDULED' AND item_count < 2)
  INTO demo_empty_day_group_count, demo_incomplete_unscheduled_count
  FROM (
    SELECT d.id, d.group_type,
           count(i.id) FILTER (WHERE i.deleted_at IS NULL) AS item_count
    FROM itinerary.itinerary_days d
    JOIN visible_trips vt ON vt.id = d.trip_id
    LEFT JOIN itinerary.itinerary_items i ON i.itinerary_day_id = d.id
    GROUP BY d.id, d.group_type
  ) groups;

  WITH demo_user AS (
    SELECT user_id FROM auth.user_email_addresses
    WHERE normalized_email = 'demo01@soomgil.local'
  )
  SELECT count(*) INTO demo_authored_post_count
  FROM community.posts p
  JOIN demo_user du ON du.user_id = p.published_by_user_id
  WHERE p.deleted_at IS NULL;

  WITH demo_user AS (
    SELECT user_id FROM auth.user_email_addresses
    WHERE normalized_email = 'demo01@soomgil.local'
  ), visible_trips AS (
    SELECT t.id
    FROM trip.trips t
    JOIN trip.trip_members tm ON tm.trip_id = t.id AND tm.status = 'ACTIVE'
    JOIN demo_user du ON du.user_id = tm.user_id
    WHERE t.status != 'DELETED'
  )
  SELECT count(*) INTO oversized_demo_trip_count
  FROM (
    SELECT vt.id
    FROM visible_trips vt
    JOIN trip.trip_members tm ON tm.trip_id = vt.id AND tm.status = 'ACTIVE'
    GROUP BY vt.id
    HAVING count(*) > 8
  ) oversized;

  SELECT count(*) INTO missing_demo_thumbnail_count
  FROM itinerary.itinerary_items i
  WHERE i.trip_id::text LIKE 'c0000000-%'
    AND i.deleted_at IS NULL
    AND i.thumbnail_url IS NULL;

  SELECT count(*) INTO missing_post_cover_count
  FROM community.posts p
  WHERE p.deleted_at IS NULL
    AND p.visibility = 'PUBLIC'
    AND p.moderation_status = 'VISIBLE'
    AND p.cover_media_file_id IS NULL;

  IF profile_count <> 120 OR distinct_bio_count <> 120 THEN
    RAISE EXCEPTION 'Expected 120 distinct demo profiles, found % profiles and % bios',
      profile_count, distinct_bio_count;
  END IF;
  IF post_count <> 59 OR distinct_title_count <> 59 OR distinct_summary_count <> 59 THEN
    RAISE EXCEPTION 'Expected 59 unique demo posts, found % posts, % titles, % summaries',
      post_count, distinct_title_count, distinct_summary_count;
  END IF;
  IF comment_count <> 320 OR distinct_comment_count <> 320 OR reply_count < 150 THEN
    RAISE EXCEPTION 'Expected 320 unique comments and at least 150 replies, found %, %, %',
      comment_count, distinct_comment_count, reply_count;
  END IF;
  IF like_count < 3000 OR distinct_like_counts <> 59 THEN
    RAISE EXCEPTION 'Expected distributed likes for every post, found % likes and % counts',
      like_count, distinct_like_counts;
  END IF;
  IF stale_url_count <> 0 THEN
    RAISE EXCEPTION 'Found % stale cdn.soomgil.test demo image URLs', stale_url_count;
  END IF;
  IF empty_demo_trip_count <> 0 THEN
    RAISE EXCEPTION 'Found % demo trips without itinerary places', empty_demo_trip_count;
  END IF;
  IF missing_demo_thumbnail_count <> 0 THEN
    RAISE EXCEPTION 'Found % demo itinerary places without thumbnails', missing_demo_thumbnail_count;
  END IF;
  IF missing_record_photo_count <> 0 THEN
    RAISE EXCEPTION 'Found % record photos without a usable CloudFront URL', missing_record_photo_count;
  END IF;
  IF portrait_record_photo_count <> 5 THEN
    RAISE EXCEPTION 'Expected 5 portrait record photos for demo01, found %', portrait_record_photo_count;
  END IF;
  IF demo_active_trip_count <> 3 OR demo_archived_trip_count <> 1 OR demo_empty_trip_count <> 0 THEN
    RAISE EXCEPTION 'Expected demo01 to have 3 active and 1 archived non-empty trips, found %, %, % empty',
      demo_active_trip_count, demo_archived_trip_count, demo_empty_trip_count;
  END IF;
  IF demo_empty_day_group_count <> 0 OR demo_incomplete_unscheduled_count <> 0 THEN
    RAISE EXCEPTION 'Expected every demo01 day group to contain places and every unscheduled group to contain 2, found % empty and % incomplete',
      demo_empty_day_group_count, demo_incomplete_unscheduled_count;
  END IF;
  IF demo_authored_post_count <> 2 THEN
    RAISE EXCEPTION 'Expected demo01 to have 2 authored posts, found %', demo_authored_post_count;
  END IF;
  IF oversized_demo_trip_count <> 0 THEN
    RAISE EXCEPTION 'Found % demo01 trips with more than 8 active members', oversized_demo_trip_count;
  END IF;
  IF missing_post_cover_count <> 0 THEN
    RAISE EXCEPTION 'Found % public demo posts without cover media', missing_post_cover_count;
  END IF;
END $$;

SELECT 'profiles' metric, 120 actual
UNION ALL SELECT 'posts', count(*) FROM community.posts
WHERE id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k))
UNION ALL SELECT 'comments', count(*) FROM community.post_comments
WHERE post_id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR post_id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k))
UNION ALL SELECT 'likes', count(*) FROM community.post_likes
WHERE post_id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR post_id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k));
