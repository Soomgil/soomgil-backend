-- 데모 데이터 v2 품질 검사. 하나라도 어긋나면 실패해 배포 전에 드러나게 한다.
\set ON_ERROR_STOP on
DO $$
DECLARE
  v_users int; v_demo_onboarded int; v_trips int; v_demo_trips int; v_empty_trips int;
  v_posts int; v_posts_without_media int; v_posts_without_cover int; v_non_kto_items int;
  v_demo1_reactions int; v_demo2_reactions int; v_votes_completed int; v_chat_trips int;
  v_checklist_trips int; v_note_trips int; v_routes int; v_comments int; v_likes int; v_follows int;
  v_stale_urls int; v_missing_avatars int; v_regions text;
BEGIN
  SELECT count(*) INTO v_users FROM auth.users WHERE status = 'ACTIVE';
  SELECT count(*) INTO v_demo_onboarded FROM auth.users u JOIN auth.user_email_addresses e ON e.user_id = u.id
    WHERE e.normalized_email IN ('demo1@soomgil.app','demo2@soomgil.app') AND u.onboarding_completed_at IS NOT NULL;
  SELECT count(*) INTO v_trips FROM trip.trips WHERE status = 'ACTIVE' AND deleted_at IS NULL;
  SELECT count(DISTINCT t.id) INTO v_demo_trips FROM trip.trips t JOIN trip.trip_members m ON m.trip_id = t.id AND m.status = 'ACTIVE'
    JOIN auth.user_email_addresses e ON e.user_id = m.user_id WHERE e.normalized_email IN ('demo1@soomgil.app','demo2@soomgil.app') AND t.deleted_at IS NULL;
  SELECT count(*) INTO v_empty_trips FROM trip.trips t WHERE t.deleted_at IS NULL
    AND NOT EXISTS (SELECT 1 FROM itinerary.itinerary_items i WHERE i.trip_id = t.id AND i.deleted_at IS NULL);
  SELECT count(*) INTO v_posts FROM community.posts WHERE deleted_at IS NULL AND visibility = 'PUBLIC';
  SELECT count(*) INTO v_posts_without_media FROM community.posts p WHERE p.deleted_at IS NULL
    AND NOT EXISTS (SELECT 1 FROM community.post_media pm WHERE pm.post_id = p.id);
  SELECT count(*) INTO v_posts_without_cover FROM community.posts WHERE deleted_at IS NULL AND cover_media_file_id IS NULL;
  SELECT count(*) INTO v_non_kto_items FROM itinerary.itinerary_items i WHERE i.deleted_at IS NULL AND i.item_type = 'PLACE'
    AND NOT EXISTS (SELECT 1 FROM tourism_source.attractions a WHERE a.content_id::text = i.external_place_id);
  SELECT count(*) INTO v_demo1_reactions FROM preference.user_place_reactions r JOIN auth.user_email_addresses e ON e.user_id = r.user_id WHERE e.normalized_email = 'demo1@soomgil.app';
  SELECT count(*) INTO v_demo2_reactions FROM preference.user_place_reactions r JOIN auth.user_email_addresses e ON e.user_id = r.user_id WHERE e.normalized_email = 'demo2@soomgil.app';
  SELECT count(*) INTO v_votes_completed FROM voting.vote_sessions WHERE status = 'COMPLETED';
  SELECT count(DISTINCT trip_id) INTO v_chat_trips FROM chat.trip_chat_messages;
  SELECT count(DISTINCT trip_id) INTO v_checklist_trips FROM planning.checklists WHERE deleted_at IS NULL;
  SELECT count(DISTINCT trip_id) INTO v_note_trips FROM planning.trip_notes WHERE deleted_at IS NULL;
  SELECT count(*) INTO v_routes FROM itinerary.trip_routes WHERE deleted_at IS NULL;
  SELECT count(*) INTO v_comments FROM community.post_comments WHERE deleted_at IS NULL;
  SELECT count(*) INTO v_likes FROM community.post_likes;
  SELECT count(*) INTO v_follows FROM social.user_follows WHERE deleted_at IS NULL;
  SELECT count(*) INTO v_stale_urls FROM (
    SELECT public_url u FROM media.media_files UNION ALL SELECT profile_image_url FROM auth.user_profiles UNION ALL SELECT thumbnail_url FROM itinerary.itinerary_items
  ) x WHERE u LIKE '%cdn.soomgil.test%' OR u LIKE '%pravatar%' OR u LIKE '%unsplash%' OR u LIKE '%picsum%';
  SELECT count(*) INTO v_missing_avatars FROM auth.user_profiles WHERE profile_image_url IS NULL;
  SELECT string_agg(DISTINCT left(r.legal_region_code, 2), ',' ORDER BY left(r.legal_region_code, 2)) INTO v_regions FROM trip.trip_regions r;

  RAISE NOTICE 'users=% demo_onboarded=% trips=% demo_trips=% empty_trips=% posts=% no_media=% no_cover=% non_kto_items=% demo1_reactions=% demo2_reactions=% votes_completed=% chat_trips=% checklist_trips=% note_trips=% routes=% comments=% likes=% follows=% stale_urls=% missing_avatars=% regions=%',
    v_users, v_demo_onboarded, v_trips, v_demo_trips, v_empty_trips, v_posts, v_posts_without_media, v_posts_without_cover, v_non_kto_items,
    v_demo1_reactions, v_demo2_reactions, v_votes_completed, v_chat_trips, v_checklist_trips, v_note_trips, v_routes, v_comments, v_likes, v_follows, v_stale_urls, v_missing_avatars, v_regions;

  IF v_users < 14 THEN RAISE EXCEPTION 'expected 14 active users, got %', v_users; END IF;
  IF v_demo_onboarded <> 2 THEN RAISE EXCEPTION 'demo accounts must have completed onboarding (got %)', v_demo_onboarded; END IF;
  IF v_demo_trips < 6 THEN RAISE EXCEPTION 'demo accounts must be members of the 6 main trips (got %)', v_demo_trips; END IF;
  IF v_empty_trips > 0 THEN RAISE EXCEPTION '% trips have no itinerary items', v_empty_trips; END IF;
  IF v_posts < 18 THEN RAISE EXCEPTION 'expected >= 18 public posts, got %', v_posts; END IF;
  IF v_posts_without_media > 0 OR v_posts_without_cover > 0 THEN RAISE EXCEPTION 'every post needs real photos (no_media=% no_cover=%)', v_posts_without_media, v_posts_without_cover; END IF;
  IF v_non_kto_items > 0 THEN RAISE EXCEPTION '% itinerary items reference unknown KTO places', v_non_kto_items; END IF;
  IF v_demo1_reactions < 20 OR v_demo2_reactions < 20 THEN RAISE EXCEPTION 'demo accounts need >= 20 swipes (demo1=% demo2=%)', v_demo1_reactions, v_demo2_reactions; END IF;
  IF v_votes_completed < 1 THEN RAISE EXCEPTION 'expected a completed vote session'; END IF;
  IF v_chat_trips < 6 OR v_checklist_trips < 6 OR v_note_trips < 6 THEN RAISE EXCEPTION 'main trips need chat/checklists/notes (chat=% checklists=% notes=%)', v_chat_trips, v_checklist_trips, v_note_trips; END IF;
  IF v_comments < 30 OR v_likes < 60 OR v_follows < 20 THEN RAISE EXCEPTION 'community engagement too thin (comments=% likes=% follows=%)', v_comments, v_likes, v_follows; END IF;
  IF v_stale_urls > 0 THEN RAISE EXCEPTION '% legacy/stock image URLs remain', v_stale_urls; END IF;
  IF v_missing_avatars > 0 THEN RAISE EXCEPTION '% users have no profile image', v_missing_avatars; END IF;
  IF v_regions IS NULL OR array_length(string_to_array(v_regions, ','), 1) < 6 THEN RAISE EXCEPTION 'trips must span 6 regions (got %)', v_regions; END IF;
  RAISE NOTICE 'demo v2 dataset OK';
END $$;
