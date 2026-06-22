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
    UNION ALL
    SELECT public_url FROM media.media_files WHERE object_key LIKE 'demo/%'
    UNION ALL
    SELECT first_image1 FROM tourism_source.attractions
    WHERE content_id BETWEEN 10001 AND 10040 OR content_id BETWEEN 20001 AND 20028
    UNION ALL
    SELECT thumbnail_url FROM itinerary.itinerary_items
    UNION ALL
    SELECT thumbnail_url FROM community.post_snapshot_items
  ) urls
  WHERE url IS NOT NULL AND url NOT LIKE 'https://daobk0bynum21.cloudfront.net/%';

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
    RAISE EXCEPTION 'Found % demo image URLs outside CloudFront', stale_url_count;
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
