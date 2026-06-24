-- Realistic local demo patch.
-- Run after soomgil_demo_seoul_daejeon.sql. It is deterministic and safe to re-run.

\set ON_ERROR_STOP on
BEGIN;

-- Give every profile a distinct combination of travel habits and interests.
WITH profile_context AS (
  SELECT u.user_id,
         row_number() OVER (ORDER BY u.user_id) AS rn,
         a.title AS favorite_place
  FROM auth.user_profiles u
  JOIN LATERAL (
    SELECT title
    FROM tourism_source.attractions
    ORDER BY md5(u.user_id::text || content_id::text)
    LIMIT 1
  ) a ON true
  WHERE u.user_id IN (SELECT md5('demo-user:' || n)::uuid FROM generate_series(1, 120) n)
), phrases AS (
  SELECT ARRAY[
    '주말 아침 첫차로 움직이는 편이에요.', '사람 적은 평일 여행을 좋아해요.',
    '맛집 하나와 산책길 하나면 충분해요.', '전시와 오래된 건물을 찾아다녀요.',
    '노을 시간에 맞춰 동선을 짜는 편입니다.', '아이와 무리 없는 코스를 기록해요.',
    '카메라 한 대 들고 골목을 오래 걷습니다.', '대중교통으로 갈 수 있는 곳을 모아요.',
    '시장과 동네 식당이 있는 여행을 좋아해요.', '숲길을 걷고 조용한 카페에서 쉬어요.',
    '친구들과 취향 투표로 일정을 정합니다.', '한 지역에 오래 머무는 느린 여행자예요.'
  ] openers,
  ARRAY[
    '요즘은 %s 주변을 더 천천히 보고 싶어요.', '%s에서 좋았던 시간을 다시 기록하는 중이에요.',
    '다음 목적지는 %s 근처로 정해두었습니다.', '%s처럼 걷기 좋은 장소를 추천받고 싶어요.',
    '%s의 계절별 풍경이 궁금합니다.', '%s에서 시작하는 반나절 코스를 만들고 있어요.',
    '최근 가장 기억에 남은 곳은 %s예요.', '%s 근처의 작은 가게를 찾는 재미에 빠졌어요.',
    '%s에 다시 가면 이번에는 골목부터 걸어볼 생각이에요.', '%s을 중심으로 친구들과 주말 계획을 짜고 있어요.'
  ] closers
)
UPDATE auth.user_profiles p
SET bio = phrases.openers[1 + ((c.rn - 1) % 12)] || ' ' ||
          format(phrases.closers[1 + (((c.rn - 1) / 12)::int % 10)], c.favorite_place),
    profile_image_url = 'https://daobk0bynum21.cloudfront.net/demo/profiles/' || p.user_id || '.png',
    updated_at = now()
FROM profile_context c CROSS JOIN phrases
WHERE p.user_id = c.user_id;

WITH names AS (
  SELECT ARRAY['하늘','서준','지민','유진','도현','서연','민재','지아','현준','수아',
    '예준','채원','도윤','예은','민준','지원','시우','나경','하준','다인',
    '재윤','소희','태윤','은서','준영','지안','승현','가은','현우','보민'] arr
), styles AS (
  SELECT ARRAY['주말산책','골목기록','느린여행','빛을담는'] arr
)
UPDATE auth.user_profiles p
SET display_name = styles.arr[1 + (((n - 21) / 30)::int % 4)] || ' ' ||
                   names.arr[1 + ((n - 21) % 30)],
    updated_at = now()
FROM generate_series(21, 120) n CROSS JOIN names CROSS JOIN styles
WHERE p.user_id = md5('demo-user:' || n)::uuid;

-- Keep demo01's dashboard focused: three active trips and one archived trip.
WITH demo_user AS (
  SELECT user_id
  FROM auth.user_email_addresses
  WHERE normalized_email = 'demo01@soomgil.local'
)
UPDATE trip.trip_members tm
SET status = 'REMOVED',
    left_at = now(),
    removed_by_user_id = NULL
FROM demo_user du
WHERE tm.user_id = du.user_id
  AND tm.status != 'REMOVED';

WITH demo_user AS (
  SELECT user_id
  FROM auth.user_email_addresses
  WHERE normalized_email = 'demo01@soomgil.local'
), visible_trips(trip_id) AS (
  VALUES
    ('dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid),
    ('c0000000-0000-4000-8000-000000000001'::uuid),
    ('c0000000-0000-4000-8000-000000000002'::uuid),
    ('c0000000-0000-4000-8000-000000000004'::uuid)
)
UPDATE trip.trip_members tm
SET status = 'ACTIVE',
    left_at = NULL,
    removed_by_user_id = NULL
FROM demo_user du, visible_trips v
WHERE tm.user_id = du.user_id
  AND tm.trip_id = v.trip_id;

-- Legacy showcase trips keep their original small party plus demo01 only.
WITH demo_users AS (
  SELECT md5('demo-user:' || n)::uuid AS user_id
  FROM generate_series(2, 120) n
), visible_legacy_trips(trip_id) AS (
  VALUES
    ('c0000000-0000-4000-8000-000000000001'::uuid),
    ('c0000000-0000-4000-8000-000000000002'::uuid),
    ('c0000000-0000-4000-8000-000000000004'::uuid)
)
UPDATE trip.trip_members tm
SET status = 'REMOVED',
    left_at = now(),
    removed_by_user_id = NULL
FROM demo_users du, visible_legacy_trips vt
WHERE tm.user_id = du.user_id
  AND tm.trip_id = vt.trip_id
  AND tm.status = 'ACTIVE';

WITH visible_trips(trip_id, trip_status) AS (
  VALUES
    ('dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid, 'ACTIVE'),
    ('c0000000-0000-4000-8000-000000000001'::uuid, 'ACTIVE'),
    ('c0000000-0000-4000-8000-000000000002'::uuid, 'ACTIVE'),
    ('c0000000-0000-4000-8000-000000000004'::uuid, 'ARCHIVED')
)
UPDATE trip.trips t
SET status = v.trip_status,
    updated_at = now()
FROM visible_trips v
WHERE t.id = v.trip_id;

-- Every visible demo01 trip has populated day groups and two concrete unscheduled options.
WITH visible_trips(trip_id, next_sort_order) AS (
  VALUES
    ('dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid, 3),
    ('c0000000-0000-4000-8000-000000000001'::uuid, 4),
    ('c0000000-0000-4000-8000-000000000002'::uuid, 3),
    ('c0000000-0000-4000-8000-000000000004'::uuid, 3)
)
INSERT INTO itinerary.itinerary_days
  (id, trip_id, group_type, day_number, date, title, sort_order, created_at, updated_at)
SELECT md5('demo-dashboard-unscheduled-day:' || v.trip_id)::uuid,
       v.trip_id, 'UNSCHEDULED', NULL, NULL, '미정', v.next_sort_order, now(), now()
FROM visible_trips v
WHERE NOT EXISTS (
  SELECT 1 FROM itinerary.itinerary_days d
  WHERE d.trip_id = v.trip_id AND d.group_type = 'UNSCHEDULED'
);

UPDATE itinerary.itinerary_days
SET title = '미정', updated_at = now()
WHERE group_type = 'UNSCHEDULED'
  AND trip_id IN (
    'dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid,
    'c0000000-0000-4000-8000-000000000001'::uuid,
    'c0000000-0000-4000-8000-000000000002'::uuid,
    'c0000000-0000-4000-8000-000000000004'::uuid
  );

UPDATE itinerary.itinerary_items i
SET deleted_at = now(), updated_at = now()
FROM itinerary.itinerary_days d
WHERE i.itinerary_day_id = d.id
  AND i.deleted_at IS NULL
  AND d.group_type = 'UNSCHEDULED'
  AND d.trip_id IN (
    'dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid,
    'c0000000-0000-4000-8000-000000000001'::uuid,
    'c0000000-0000-4000-8000-000000000002'::uuid,
    'c0000000-0000-4000-8000-000000000004'::uuid
  );

WITH places(trip_id, sort_order, external_place_id, place_name, address, lat, lng) AS (
  VALUES
    ('dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid, 0, 'dashboard-seoul-forest', '서울숲', '서울특별시 성동구 뚝섬로 273', 37.5444000, 127.0374000),
    ('dee764a6-e8d4-dc6c-7766-b4e13a939ea4'::uuid, 1, 'dashboard-seongsu-cafe', '성수동 카페거리', '서울특별시 성동구 성수동2가', 37.5446000, 127.0557000),
    ('c0000000-0000-4000-8000-000000000001'::uuid, 0, 'dashboard-hyeopjae', '협재해수욕장', '제주특별자치도 제주시 한림읍 협재리', 33.3940000, 126.2397000),
    ('c0000000-0000-4000-8000-000000000001'::uuid, 1, 'dashboard-camellia', '카멜리아힐', '제주특별자치도 서귀포시 안덕면 병악로 166', 33.2897000, 126.3689000),
    ('c0000000-0000-4000-8000-000000000002'::uuid, 0, 'dashboard-songdo-cablecar', '송도해상케이블카', '부산광역시 서구 송도해변로 171', 35.0768000, 129.0239000),
    ('c0000000-0000-4000-8000-000000000002'::uuid, 1, 'dashboard-huinnyeoul', '흰여울문화마을', '부산광역시 영도구 영선동4가 605-3', 35.0787000, 129.0443000),
    ('c0000000-0000-4000-8000-000000000004'::uuid, 0, 'dashboard-donggung', '동궁과 월지', '경상북도 경주시 원화로 102', 35.8348000, 129.2266000),
    ('c0000000-0000-4000-8000-000000000004'::uuid, 1, 'dashboard-hwangridan', '황리단길', '경상북도 경주시 포석로 일대', 35.8380000, 129.2090000)
), unscheduled_days AS (
  SELECT d.id, d.trip_id
  FROM itinerary.itinerary_days d
  WHERE d.group_type = 'UNSCHEDULED'
)
INSERT INTO itinerary.itinerary_items
  (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider,
   external_place_id, place_name, address, lat, lng, thumbnail_url, source_status,
   created_at, updated_at)
SELECT md5('demo-dashboard-unscheduled-place:' || p.external_place_id)::uuid,
       p.trip_id, d.id, p.sort_order, 'PLACE', 'KTO', p.external_place_id,
       p.place_name, p.address, p.lat, p.lng,
       'https://daobk0bynum21.cloudfront.net/demo/legacy-places/' ||
         md5('KTO:' || p.external_place_id) || '/cover.jpg',
       'AVAILABLE', now(), now()
FROM places p
JOIN unscheduled_days d ON d.trip_id = p.trip_id
ON CONFLICT (id) DO UPDATE SET
  itinerary_day_id = EXCLUDED.itinerary_day_id,
  sort_order = EXCLUDED.sort_order,
  place_provider = EXCLUDED.place_provider,
  external_place_id = EXCLUDED.external_place_id,
  place_name = EXCLUDED.place_name,
  address = EXCLUDED.address,
  lat = EXCLUDED.lat,
  lng = EXCLUDED.lng,
  thumbnail_url = EXCLUDED.thumbnail_url,
  source_status = 'AVAILABLE',
  deleted_at = NULL,
  updated_at = now();

-- Normalize legacy demo place references to the only supported place provider.
UPDATE itinerary.itinerary_items
SET place_provider = 'KTO',
    updated_at = now()
WHERE place_provider = 'KAKAO';

UPDATE community.post_snapshot_items
SET place_provider = 'KTO'
WHERE place_provider = 'KAKAO';

-- Attribute two complete Seoul stories to demo01 without inflating the global post count.
WITH demo_user AS (
  SELECT user_id
  FROM auth.user_email_addresses
  WHERE normalized_email = 'demo01@soomgil.local'
)
UPDATE community.posts p
SET published_by_user_id = du.user_id,
    updated_at = now()
FROM demo_user du
WHERE p.id IN (
  md5('demo-post:palace-post')::uuid,
  md5('demo-post:night-post')::uuid
);

WITH demo_user AS (
  SELECT user_id
  FROM auth.user_email_addresses
  WHERE normalized_email = 'demo01@soomgil.local'
)
UPDATE media.media_files m
SET owner_user_id = du.user_id
FROM demo_user du
WHERE m.linked_resource_type = 'COMMUNITY_POST'
  AND m.linked_resource_id IN (
    md5('demo-post:palace-post')::uuid,
    md5('demo-post:night-post')::uuid
  );

-- Replace generated bulk titles and summaries with context-aware, individually different writing.
WITH post_context AS (
  SELECT g, p.id, p.source_trip_id, t.display_destination,
         COALESCE(items.place1, t.display_destination) AS place1,
         COALESCE(items.place2, items.place1, t.display_destination) AS place2
  FROM generate_series(1, 50) g
  JOIN community.posts p ON p.id = md5('demo-bulk-post:' || g)::uuid
  JOIN trip.trips t ON t.id = p.source_trip_id
  LEFT JOIN LATERAL (
    SELECT max(i.place_name) FILTER (WHERE i.rn = 1) AS place1,
           max(i.place_name) FILTER (WHERE i.rn = 2) AS place2
    FROM (
      SELECT ii.place_name, row_number() OVER (ORDER BY d.sort_order, ii.sort_order) rn
      FROM itinerary.itinerary_items ii
      JOIN itinerary.itinerary_days d ON d.id = ii.itinerary_day_id
      WHERE ii.trip_id = p.source_trip_id AND ii.deleted_at IS NULL
      LIMIT 2
    ) i
  ) items ON true
), copy AS (
  SELECT ARRAY[
    '%s 문 열자마자 들어가서 시작한 하루', '%s에서 뜻밖에 오래 머문 이유',
    '친구 셋이 직접 걸어본 %s 하루 코스', '비 오는 날에도 괜찮았던 %s 여행',
    '%s부터 %s까지, 서두르지 않은 주말', '사진보다 현장이 좋았던 %s 산책',
    '대중교통으로만 다녀온 %s 1박 2일', '%s 근처에서 찾은 조용한 장소들',
    '계획 절반만 지켰는데 더 좋았던 %s', '부모님과 천천히 둘러본 %s 여행'
  ] titles,
  ARRAY[
    ', 아침부터 늦은 점심까지', ', 비워둔 오후가 좋았던 날', ', 오래 남은 대화와 풍경',
    ', 예상 밖의 골목을 만나다', ', 다음 계절에 다시 걷고 싶은 길'
  ] title_endings,
  ARRAY[
    '%s은 오전에 가니 기다림이 짧았고, %s까지는 걸어서 이동했어요. 실제 소요 시간과 쉬어가기 좋았던 곳을 함께 적었습니다.',
    '유명한 장소를 많이 넣기보다 %s과 %s에 오래 머물렀습니다. 직접 걸어보니 좋았던 순서와 아쉬웠던 점까지 솔직하게 남겨요.',
    '%s에서 시작해 %s으로 넘어간 동선입니다. 일행마다 속도가 달라도 무리 없도록 중간 휴식 시간을 넉넉히 잡았어요.',
    '날씨가 흐려 계획을 바꿨지만 %s과 %s 덕분에 오히려 여유로운 하루가 됐어요. 비슷한 날씨에 쓸 수 있는 대안도 담았습니다.',
    '%s 주변은 점심 이후 붐벼 조금 일찍 움직였고, %s에서는 해 질 때까지 머물렀어요. 시간대별 분위기를 기록했습니다.',
    '처음 가는 사람도 따라가기 쉽도록 %s에서 %s까지 교통편과 도보 구간을 나눠 적었어요. 불필요했던 이동도 표시했습니다.',
    '%s을 중심으로 가까운 장소만 묶었습니다. %s까지 둘러봐도 일정이 빡빡하지 않아 대화하며 걷기 좋았어요.',
    '사진 촬영보다 함께 보내는 시간에 집중한 여행입니다. %s과 %s에서 기억에 남은 작은 순간들을 코스와 함께 정리했어요.',
    '%s은 기대 이상이었고 %s은 예상보다 사람이 많았습니다. 다음에 다시 간다면 바꾸고 싶은 순서도 덧붙였습니다.',
    '현지에서 추천받은 %s을 넣고 %s 주변 골목까지 걸었습니다. 관광지만 빠르게 도는 여행보다 동네 분위기를 느끼고 싶은 분께 맞아요.'
  ] summaries,
  ARRAY[
    ' 첫 방문에서 놓치기 쉬운 부분도 마지막에 따로 정리했습니다.',
    ' 실제 지출과 이동 시간은 일정 메모에서 확인할 수 있어요.',
    ' 일행 셋의 서로 다른 후기까지 장소별 기록에 남겼습니다.',
    ' 다시 간다면 바꾸고 싶은 한 가지도 숨기지 않고 적었어요.',
    ' 사진을 찍은 시간과 쉬어간 지점은 기록 탭에 표시해두었습니다.'
  ] summary_endings
)
UPDATE community.posts p
SET title = format(copy.titles[1 + ((c.g - 1) % 10)], c.place1, c.place2) ||
            copy.title_endings[1 + (((c.g - 1) / 10)::int % 5)],
    summary = format(copy.summaries[1 + (((c.g * 7) - 1) % 10)], c.place1, c.place2) ||
              copy.summary_endings[1 + (((c.g - 1) / 10)::int % 5)],
    updated_at = now()
FROM post_context c CROSS JOIN copy
WHERE p.id = c.id;

-- Remove the eight repeated expansion comments and rebuild six contextual comments per post.
DELETE FROM community.post_comments
WHERE post_id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g);

WITH post_context AS (
  SELECT g, p.id AS post_id, p.published_by_user_id, p.published_at, t.display_destination,
         COALESCE(items.place1, t.display_destination) AS place1,
         COALESCE(items.place2, items.place1, t.display_destination) AS place2
  FROM generate_series(1, 50) g
  JOIN community.posts p ON p.id = md5('demo-bulk-post:' || g)::uuid
  JOIN trip.trips t ON t.id = p.source_trip_id
  LEFT JOIN LATERAL (
    SELECT max(i.place_name) FILTER (WHERE i.rn = 1) AS place1,
           max(i.place_name) FILTER (WHERE i.rn = 2) AS place2
    FROM (
      SELECT ii.place_name, row_number() OVER (ORDER BY d.sort_order, ii.sort_order) rn
      FROM itinerary.itinerary_items ii
      JOIN itinerary.itinerary_days d ON d.id = ii.itinerary_day_id
      WHERE ii.trip_id = p.source_trip_id AND ii.deleted_at IS NULL
      LIMIT 2
    ) i
  ) items ON true
), copy AS (
  SELECT ARRAY[
    '%s은 몇 시쯤 도착하셨어요? 주말 대기 시간이 궁금해요.', '%s에서 %s까지 실제로 걸으면 얼마나 걸렸나요?',
    '%s 주변에서 점심 먹기 괜찮았던 곳도 있었나요?', '%s은 아이와 같이 가도 이동이 힘들지 않을까요?',
    '%s 사진 빛이 정말 좋네요. 촬영 시간이 언제였는지 궁금해요.', '%s 근처 주차보다 대중교통이 더 편할까요?',
    '%s을 비 오는 날 가도 충분히 즐길 수 있을까요?', '%s에서 가장 오래 머문 구간은 어디였나요?',
    '%s 다음에 %s을 넣은 순서가 좋아 보여요. 반대로 가도 괜찮을까요?', '%s은 예약 없이 방문해도 되는지 알려주실 수 있나요?'
  ] questions,
  ARRAY[
    '%s은 생각보다 한적했어요. 코스 전체 분위기가 편안해 보여 저장했습니다.', '%s과 %s을 같은 날 묶을 생각을 못 했는데 동선이 자연스럽네요.',
    '저도 지난달 %s에 갔는데 계절이 달라서 사진 분위기가 완전히 다르네요.', '%s 근처는 잠깐 들르기만 했는데 다음에는 천천히 둘러봐야겠어요.',
    '이동 시간을 과장하지 않고 적어주셔서 계획 세우는 데 도움이 됐어요.', '%s에서 쉬어간 선택이 좋아 보여요. 일행에게 바로 공유했습니다.',
    '장소를 많이 넣지 않아 실제로 따라가기 좋은 일정 같아요.', '%s 주변 골목 이야기가 반갑네요. 저도 거기서 예상보다 오래 머물렀어요.',
    '사진만 봐도 그날 날씨와 분위기가 느껴져요. 기록 잘 보고 갑니다.', '%s 코스는 부모님과 가기에도 좋아 보여서 리트립했어요.'
  ] reactions,
  ARRAY[
    '%s 근처에서는 작은 골목으로 한 블록만 들어가도 훨씬 조용해요.', '%s은 마감 한 시간 전보다 오전 방문이 여유로웠습니다.',
    '%s에서 %s 방향으로 갈 때 버스보다 지하철 환승이 빨랐어요.', '%s 주변 공용 보관함을 쓰면 짐 들고 걷지 않아도 돼요.',
    '%s은 햇빛이 강해서 여름에는 모자나 양산을 챙기는 게 좋습니다.', '%s 근처 카페는 오후 두 시 전후로 자리가 빨리 차더라고요.',
    '%s은 평일 마지막 입장 시간도 꼭 확인하고 가세요.', '%s 주변은 저녁이 되면 분위기가 달라져 한 번 더 걷기 좋아요.',
    '%s에서 사진 찍을 때 정문보다 옆 산책로가 덜 붐볐어요.', '%s과 %s 사이에 잠깐 쉴 벤치가 많아 천천히 이동해도 괜찮습니다.'
  ] tips,
  ARRAY[
    '이번 주 토요일에 가려고 해서 미리 여쭤봐요.',
    '다음 달 부모님과 가기 전에 확인하고 싶어요.',
    '친구들과 오전 일정으로 잡아두었습니다.',
    '비슷한 코스로 혼자 걸어볼 계획이에요.',
    '기차 시간에 맞춰 동선을 조정하는 중입니다.'
  ] question_contexts,
  ARRAY[
    '주말 계획방에 바로 공유했어요.',
    '사진 좋아하는 친구가 특히 좋아할 것 같아요.',
    '다음 달 일정 후보에 넣어두었습니다.',
    '저희 일행 취향에도 잘 맞을 것 같아요.',
    '비슷한 속도로 여행하는 분들께도 추천하고 싶네요.'
  ] reaction_contexts,
  ARRAY[
    '지난주에 다녀온 기억이 나서 남겨봅니다.',
    '근처에서 일하는 친구에게 들은 내용도 같았어요.',
    '처음 가는 분들이 놓치기 쉬운 부분이더라고요.',
    '계절이 바뀌면 운영 시간이 달라질 수도 있어요.',
    '저희 일행도 이 정보 덕분에 이동이 편했습니다.'
  ] tip_contexts,
  ARRAY[
    '저희는 오전 %s부터 시작해서 기다림이 거의 없었어요. 점심 이후에는 사람이 조금 늘었습니다.',
    '%s까지 사진 찍으며 천천히 걸어서 약 한 시간 정도 걸렸어요. 빠르게 걸으면 더 짧아요.',
    '%s에서 두 블록 정도 떨어진 작은 식당을 이용했어요. 게시글 일정 메모에도 위치를 추가해둘게요.',
    '경사가 심한 구간은 없었지만 %s에서 한 번 쉬어가면 아이도 무리 없을 것 같아요.',
    '해 지기 한 시간 전쯤 %s에 도착했어요. 흐린 날에는 조금 더 일찍 가는 편이 좋습니다.',
    '저희는 대중교통을 이용했고 %s 입구까지 걷는 길도 어렵지 않았어요.',
    '실내 구간이 있어 괜찮지만 %s 주변 산책은 우산이 필요해요. 비가 세면 순서를 바꾸는 걸 추천합니다.',
    '%s에서 전시와 주변 길까지 합쳐 두 시간 가까이 머물렀어요.',
    '반대 순서도 가능하지만 저녁 풍경은 %s 쪽이 더 좋아서 지금 순서를 추천해요.',
    '저희가 갔을 때는 현장 입장이 가능했어요. 다만 주말에는 %s 공식 안내를 한 번 확인하는 게 안전합니다.'
  ] answers
), roots AS (
  SELECT c.*, slot,
         CASE slot
           WHEN 1 THEN format(copy.questions[1 + ((c.g - 1) % 10)], c.place1, c.place2)
           WHEN 2 THEN format(copy.reactions[1 + (((c.g * 3) - 1) % 10)], c.place1, c.place2)
           ELSE format(copy.tips[1 + (((c.g * 7) - 1) % 10)], c.place1, c.place2)
         END || ' ' || CASE slot
           WHEN 1 THEN copy.question_contexts[1 + (((c.g - 1) / 10)::int % 5)]
           WHEN 2 THEN copy.reaction_contexts[1 + (((c.g - 1) / 10)::int % 5)]
           ELSE copy.tip_contexts[1 + (((c.g - 1) / 10)::int % 5)]
         END AS content
  FROM post_context c CROSS JOIN generate_series(1, 3) slot CROSS JOIN copy
)
INSERT INTO community.post_comments
  (id, post_id, parent_comment_id, author_user_id, content, depth, moderation_status, created_at, updated_at)
SELECT md5('demo-real-comment:' || g || ':' || slot)::uuid, post_id, NULL,
       md5('demo-user:' || (1 + ((g * 19 + slot * 23) % 120)))::uuid,
       content, 0, 'VISIBLE', published_at + make_interval(hours => 5 + slot * 7),
       published_at + make_interval(hours => 5 + slot * 7)
FROM roots
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, updated_at = EXCLUDED.updated_at;

WITH post_context AS (
  SELECT g, p.id AS post_id, p.published_by_user_id, p.published_at,
         COALESCE(items.place1, t.display_destination) AS place1,
         COALESCE(items.place2, items.place1, t.display_destination) AS place2
  FROM generate_series(1, 50) g
  JOIN community.posts p ON p.id = md5('demo-bulk-post:' || g)::uuid
  JOIN trip.trips t ON t.id = p.source_trip_id
  LEFT JOIN LATERAL (
    SELECT max(i.place_name) FILTER (WHERE i.rn = 1) AS place1,
           max(i.place_name) FILTER (WHERE i.rn = 2) AS place2
    FROM (
      SELECT ii.place_name, row_number() OVER (ORDER BY d.sort_order, ii.sort_order) rn
      FROM itinerary.itinerary_items ii JOIN itinerary.itinerary_days d ON d.id = ii.itinerary_day_id
      WHERE ii.trip_id = p.source_trip_id AND ii.deleted_at IS NULL LIMIT 2
    ) i
  ) items ON true
), answers AS (
  SELECT ARRAY[
    '오전 일찍 움직여 기다림이 거의 없었어요. %s은 점심 무렵부터 사람이 조금 늘었습니다.',
    '사진 찍으며 천천히 걸어서 한 시간 정도 걸렸어요. %s에서 쉬어간 시간은 제외했습니다.',
    '%s 근처 골목의 작은 식당을 이용했어요. 위치를 일정 메모에 추가해둘게요.',
    '경사가 심하지는 않았지만 %s에서 한 번 쉬어가니 훨씬 편했습니다.',
    '해 지기 한 시간 전쯤 %s에 도착했어요. 그때부터 빛이 부드러워졌습니다.',
    '저희는 대중교통을 이용했고 %s 입구까지 걷는 길도 어렵지 않았어요.',
    '실내 구간은 괜찮지만 %s 주변을 걸으려면 우산이 필요해요.',
    '%s에서 전시와 주변 길까지 합쳐 두 시간 가까이 머물렀습니다.',
    '반대 순서도 가능하지만 저녁 풍경은 %s 쪽이 더 좋아 지금 순서를 추천해요.',
    '저희가 갔을 때는 현장 입장이 가능했어요. 주말에는 %s 공식 안내를 확인하는 게 안전합니다.'
  ] arr,
  ARRAY[
    '이번에는 친구 둘과 함께 움직였습니다.',
    '저희 일행은 걷는 속도가 조금 느린 편이에요.',
    '사진을 좋아하는 친구와 둘이 다녀왔습니다.',
    '아이와 함께라 휴식 시간을 넉넉히 잡았어요.',
    '부모님과 가는 일정이라 이동을 짧게 나눴습니다.',
    '세 명이 각자 보고 싶은 곳을 하나씩 골랐어요.',
    '혼자 다녀온 일정이라 현장에서 순서를 자주 바꿨습니다.',
    '오랜만에 만난 친구와 대화하며 천천히 걸었어요.',
    '당일치기라 돌아오는 기차 시간을 먼저 정해두었습니다.',
    '짐이 많지 않아 대부분 구간을 걸어서 이동했어요.'
  ] details,
  ARRAY[
    '도움이 되었으면 좋겠어요.',
    '다녀오신 뒤 분위기가 어땠는지도 궁금하네요.',
    '일정이 바뀌면 본문 메모도 같이 고쳐둘게요.',
    '무리하지 말고 중간에 꼭 쉬어가세요.',
    '교통편은 당일에 한 번 더 확인하는 걸 추천해요.'
  ] contexts
), replies AS (
  SELECT c.*, slot,
         CASE slot
           WHEN 1 THEN format(answers.arr[1 + ((c.g - 1) % 10)], c.place1, c.place2)
           WHEN 2 THEN format('맞아요. 특히 %s에서 잠깐 쉬어가니 일정이 훨씬 여유로웠어요. 다음에는 다른 계절에도 가보려고요.', c.place2)
           ELSE format('추가 팁 감사합니다! %s 주변 정보는 본문 일정 메모에도 반영해두었습니다.', c.place1)
         END || ' ' || answers.details[1 + ((c.g + slot * 3 - 1) % 10)] || ' ' ||
         answers.contexts[1 + ((((c.g - 1) / 10)::int + slot - 1) % 5)] AS content
  FROM post_context c CROSS JOIN generate_series(1, 3) slot CROSS JOIN answers
)
INSERT INTO community.post_comments
  (id, post_id, parent_comment_id, author_user_id, content, depth, moderation_status, created_at, updated_at)
SELECT md5('demo-real-reply:' || g || ':' || slot)::uuid, post_id,
       md5('demo-real-comment:' || g || ':' || slot)::uuid,
       CASE WHEN slot IN (1, 3) THEN published_by_user_id
            ELSE md5('demo-user:' || (1 + ((g * 29 + 41) % 120)))::uuid END,
       content, 1, 'VISIBLE', published_at + make_interval(hours => 9 + slot * 7),
       published_at + make_interval(hours => 9 + slot * 7)
FROM replies
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, updated_at = EXCLUDED.updated_at;

-- Turn the authored base exchanges into actual parent/reply relationships.
UPDATE community.post_comments reply
SET parent_comment_id = parent.id, depth = 1
FROM community.post_comments parent
WHERE (reply.id, parent.id) IN (
  VALUES
    (md5('demo-comment:palace-post:1:1')::uuid, md5('demo-comment:palace-post:11:1')::uuid),
    (md5('demo-comment:night-post:5:1')::uuid, md5('demo-comment:night-post:10:1')::uuid),
    (md5('demo-comment:seongsu-post:3:2')::uuid, md5('demo-comment:seongsu-post:15:2')::uuid),
    (md5('demo-comment:science-post:12:1')::uuid, md5('demo-comment:science-post:6:1')::uuid),
    (md5('demo-comment:green-post:8:1')::uuid, md5('demo-comment:green-post:17:1')::uuid),
    (md5('demo-comment:modern-post:7:4')::uuid, md5('demo-comment:modern-post:16:4')::uuid)
);

-- Give every demo post a different, believable like count and time distribution.
DELETE FROM community.post_likes
WHERE post_id IN (
  SELECT md5('demo-post:' || k)::uuid FROM (VALUES
    ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
    ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')
  ) v(k)
  UNION ALL
  SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g
);

WITH demo_posts AS (
  SELECT p.id, p.published_by_user_id, p.published_at,
         row_number() OVER (ORDER BY p.published_at, p.id) AS rn
  FROM community.posts p
  WHERE p.id IN (
    SELECT md5('demo-post:' || k)::uuid FROM (VALUES
      ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
      ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')
    ) v(k)
    UNION ALL
    SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g
  )
), candidates AS (
  SELECT p.*, u,
         row_number() OVER (PARTITION BY p.id ORDER BY md5(p.id::text || u::text)) AS like_rank,
         11 + ((p.rn * 37 + 13) % 91) AS target_count
  FROM demo_posts p CROSS JOIN generate_series(1, 120) u
  WHERE md5('demo-user:' || u)::uuid <> p.published_by_user_id
)
INSERT INTO community.post_likes(post_id, user_id, created_at)
SELECT id, md5('demo-user:' || u)::uuid,
       published_at + make_interval(hours => (like_rank * 5 + rn * 3)::int)
FROM candidates
WHERE like_rank <= target_count
ON CONFLICT DO NOTHING;

-- Diversify trip record writing using its actual place and capture time.
WITH copy AS (
  SELECT ARRAY[
    '%s에서 시작한 느린 아침', '%s, 계획보다 오래 머문 곳', '%s에서 우리끼리 찾은 장면',
    '비가 그친 뒤의 %s', '%s 골목 끝에서 만난 풍경', '%s에서 잠깐 쉬어간 시간',
    '해 질 무렵 다시 본 %s', '%s에서 남긴 여행의 온도', '%s, 돌아와서도 생각난 순간',
    '사람이 빠진 뒤의 %s', '%s에서 우연히 맞은 좋은 빛', '다음에도 걷고 싶은 %s'
  ] titles,
  ARRAY[
    '예정보다 일찍 도착해 조용한 %s을 천천히 둘러봤다. 서두르지 않아 주변 소리까지 오래 기억에 남는다.',
    '%s에서 사진을 찍다가 근처 길까지 걷게 됐다. 계획에는 없었지만 이번 여행에서 가장 좋았던 시간.',
    '함께 간 사람마다 좋았던 지점이 달랐던 %s. 잠깐 앉아 이야기를 나눈 순간이 사진보다 선명하다.',
    '날씨 때문에 순서를 바꿔 도착한 %s. 덕분에 사람이 적어 오히려 천천히 볼 수 있었다.',
    '%s을 보고 바로 이동하려 했지만 주변 풍경이 좋아 한참 더 머물렀다. 다음에는 다른 계절에 와보고 싶다.',
    '긴 이동 뒤 %s에서 쉬어갔다. 일정 사이에 비워둔 시간이 여행의 가장 좋은 장면이 되었다.',
    '%s에 도착했을 때 빛이 조금씩 바뀌고 있었다. 같은 장소도 시간에 따라 분위기가 완전히 달랐다.',
    '친구가 먼저 발견한 %s의 작은 풍경. 다 같이 걸었기에 남길 수 있었던 기록이다.',
    '%s은 기대했던 모습과 달랐지만 그래서 더 기억에 남는다. 직접 와봐야 알 수 있는 분위기가 있었다.',
    '마감이 가까워진 %s은 낮보다 조용했다. 짧게 보려던 계획을 바꿔 마지막까지 머물렀다.'
  ] captions
)
UPDATE record.trip_record_entries r
SET title = format(copy.titles[1 + (abs(hashtext(r.id::text)) % 12)], r.location_name),
    caption = format(copy.captions[1 + (abs(hashtext(r.id::text || 'caption')) % 10)], r.location_name),
    updated_at = now()
FROM copy
WHERE r.id IN (
  SELECT md5('demo-record:' || i.id)::uuid FROM itinerary.itinerary_items i
  UNION ALL
  SELECT md5('demo-bulk-record:' || i.id)::uuid FROM itinerary.itinerary_items i
);

UPDATE record.trip_record_media rm
SET caption = r.title
FROM record.trip_record_entries r
WHERE rm.record_entry_id = r.id
  AND (r.id IN (SELECT md5('demo-record:' || i.id)::uuid FROM itinerary.itinerary_items i)
       OR r.id IN (SELECT md5('demo-bulk-record:' || i.id)::uuid FROM itinerary.itinerary_items i));

-- Add portrait photos to demo01's Seoul records so the masonry feed exercises mixed ratios.
WITH demo_user AS (
  SELECT user_id
  FROM auth.user_email_addresses
  WHERE normalized_email = 'demo01@soomgil.local'
), portrait_targets AS (
  SELECT DISTINCT ON (r.id) r.id AS record_id, r.uploaded_by_user_id
  FROM record.trip_record_entries r
  JOIN record.trip_record_media rm ON rm.record_entry_id = r.id
  JOIN media.media_files existing ON existing.id = rm.media_file_id
  JOIN demo_user du ON du.user_id = r.uploaded_by_user_id
  JOIN trip.trips t ON t.id = r.trip_id
  WHERE r.status = 'ACTIVE'
    AND t.display_destination LIKE '서울%'
    AND existing.public_url IS NOT NULL
    AND existing.width >= existing.height
  ORDER BY r.id, existing.created_at
  LIMIT 5
)
INSERT INTO media.media_files
  (id, owner_user_id, storage_provider, bucket, object_key, public_url, mime_type,
   byte_size, width, height, linked_resource_type, linked_resource_id, status, created_at)
SELECT md5('demo-portrait-media:' || record_id)::uuid,
       uploaded_by_user_id,
       'S3_COMPATIBLE',
       'soomgil-media-dev-337872593610-ap-northeast-2-an',
       'demo/records/' || record_id || '/portrait-v2.jpg',
       'https://daobk0bynum21.cloudfront.net/demo/records/' || record_id || '/portrait-v2.jpg',
       'image/jpeg', 420000, 900, 1350, 'TRIP_RECORD', record_id, 'ACTIVE', now()
FROM portrait_targets
ON CONFLICT (id) DO UPDATE SET
  bucket = EXCLUDED.bucket,
  object_key = EXCLUDED.object_key,
  public_url = EXCLUDED.public_url,
  mime_type = EXCLUDED.mime_type,
  width = EXCLUDED.width,
  height = EXCLUDED.height,
  status = 'ACTIVE',
  deleted_at = NULL,
  purge_after_at = NULL,
  purged_at = NULL;

WITH demo_user AS (
  SELECT user_id
  FROM auth.user_email_addresses
  WHERE normalized_email = 'demo01@soomgil.local'
), portrait_targets AS (
  SELECT DISTINCT ON (r.id) r.id AS record_id
  FROM record.trip_record_entries r
  JOIN record.trip_record_media rm ON rm.record_entry_id = r.id
  JOIN media.media_files existing ON existing.id = rm.media_file_id
  JOIN demo_user du ON du.user_id = r.uploaded_by_user_id
  JOIN trip.trips t ON t.id = r.trip_id
  WHERE r.status = 'ACTIVE'
    AND t.display_destination LIKE '서울%'
    AND existing.public_url IS NOT NULL
    AND existing.width >= existing.height
  ORDER BY r.id, existing.created_at
  LIMIT 5
)
INSERT INTO record.trip_record_media
  (record_entry_id, media_file_id, sort_order, caption, created_at)
SELECT record_id,
       md5('demo-portrait-media:' || record_id)::uuid,
       1,
       '세로 구도로 남긴 여행 순간',
       now()
FROM portrait_targets
ON CONFLICT (record_entry_id, media_file_id) DO UPDATE SET caption = EXCLUDED.caption;

-- Move one repeated landscape record photo to a fresh object key for cache-safe replacement.
UPDATE media.media_files
SET object_key = 'demo/records/babfeef3-afc7-faa3-2a4a-7b4d8f494a52/cover-v2.jpg',
    public_url = 'https://daobk0bynum21.cloudfront.net/demo/records/babfeef3-afc7-faa3-2a4a-7b4d8f494a52/cover-v2.jpg'
WHERE id = '2cbad789-f9b3-bbc8-e43a-fef0af5af740';

-- All demo URLs point to real objects uploaded by sync-demo-media.py.
UPDATE auth.user_profiles p
SET profile_image_url = 'https://daobk0bynum21.cloudfront.net/demo/profiles/' || p.user_id || '.png',
    updated_at = now()
WHERE p.user_id::text LIKE 'a0000000-%';

UPDATE media.media_files m
SET bucket = 'soomgil-media-dev-337872593610-ap-northeast-2-an',
    object_key = 'demo/profiles/' || m.linked_resource_id || '.png',
    public_url = 'https://daobk0bynum21.cloudfront.net/demo/profiles/' || m.linked_resource_id || '.png',
    mime_type = 'image/png'
WHERE m.linked_resource_type = 'auth.users'
  AND m.linked_resource_id::text LIKE 'a0000000-%';

UPDATE media.media_files m
SET bucket = 'soomgil-media-dev-337872593610-ap-northeast-2-an',
    object_key = 'demo/trips/' || m.linked_resource_id || '/' || m.id || '.jpg',
    public_url = 'https://daobk0bynum21.cloudfront.net/demo/trips/' || m.linked_resource_id || '/' || m.id || '.jpg'
WHERE m.linked_resource_type = 'trip.trips'
  AND m.linked_resource_id::text LIKE 'c0000000-%';

UPDATE tourism_source.attractions
SET first_image1 = 'https://daobk0bynum21.cloudfront.net/demo/places/' || content_id || '/cover.jpg',
    first_image2 = 'https://daobk0bynum21.cloudfront.net/demo/places/' || content_id || '/detail.jpg'
WHERE content_id BETWEEN 10001 AND 10040 OR content_id BETWEEN 20001 AND 20028;

UPDATE tourism_source.attraction_images ai
SET storage_provider = 'S3_COMPATIBLE',
    bucket = 'soomgil-media-dev-337872593610-ap-northeast-2-an',
    object_key = 'demo/places/' || a.content_id || '/cover.jpg',
    public_url = 'https://daobk0bynum21.cloudfront.net/demo/places/' || a.content_id || '/cover.jpg',
    updated_at = now()
FROM tourism_source.attractions a
WHERE ai.attraction_no = a.no
  AND (a.content_id BETWEEN 10001 AND 10040 OR a.content_id BETWEEN 20001 AND 20028);

UPDATE media.media_files
SET bucket = 'soomgil-media-dev-337872593610-ap-northeast-2-an',
    public_url = 'https://daobk0bynum21.cloudfront.net/' || object_key
WHERE object_key LIKE 'demo/%';

WITH posts_without_media AS (
  SELECT p.*
  FROM community.posts p
  WHERE p.deleted_at IS NULL
    AND p.cover_media_file_id IS NULL
    AND NOT EXISTS (SELECT 1 FROM community.post_media pm WHERE pm.post_id = p.id)
)
INSERT INTO media.media_files
  (id, owner_user_id, storage_provider, bucket, object_key, public_url, mime_type,
   byte_size, width, height, linked_resource_type, linked_resource_id, status, created_at)
SELECT md5('demo-legacy-community-cover:' || p.id)::uuid,
       p.published_by_user_id,
       'S3_COMPATIBLE',
       'soomgil-media-dev-337872593610-ap-northeast-2-an',
       'demo/legacy-community/' || p.id || '/cover.jpg',
       'https://daobk0bynum21.cloudfront.net/demo/legacy-community/' || p.id || '/cover.jpg',
       'image/jpeg', 420000, 1400, 900, 'COMMUNITY_POST', p.id, 'ACTIVE', p.published_at
FROM posts_without_media p
ON CONFLICT (id) DO UPDATE SET
  object_key = EXCLUDED.object_key,
  public_url = EXCLUDED.public_url,
  bucket = EXCLUDED.bucket;

WITH posts_without_media AS (
  SELECT p.*
  FROM community.posts p
  WHERE p.deleted_at IS NULL
    AND p.cover_media_file_id IS NULL
    AND NOT EXISTS (SELECT 1 FROM community.post_media pm WHERE pm.post_id = p.id)
)
INSERT INTO community.post_media
  (id, post_id, media_file_id, sort_order, caption, created_at)
SELECT md5('demo-legacy-community-post-media:' || p.id)::uuid,
       p.id,
       md5('demo-legacy-community-cover:' || p.id)::uuid,
       0,
       p.title,
       p.published_at
FROM posts_without_media p
ON CONFLICT (id) DO NOTHING;

UPDATE community.posts p
SET cover_media_file_id = (
      SELECT pm.media_file_id
      FROM community.post_media pm
      WHERE pm.post_id = p.id
      ORDER BY pm.sort_order, pm.created_at
      LIMIT 1
    ),
    updated_at = now()
WHERE p.cover_media_file_id IS NULL
  AND EXISTS (SELECT 1 FROM community.post_media pm WHERE pm.post_id = p.id);

UPDATE itinerary.itinerary_items i
SET thumbnail_url = a.first_image1, updated_at = now()
FROM tourism_source.attractions a
WHERE i.place_provider = 'KTO' AND i.external_place_id = a.content_id::text
  AND (a.content_id BETWEEN 10001 AND 10040 OR a.content_id BETWEEN 20001 AND 20028);

UPDATE itinerary.itinerary_items i
SET thumbnail_url = 'https://daobk0bynum21.cloudfront.net/demo/legacy-places/' ||
      md5(COALESCE(i.place_provider, 'CUSTOM') || ':' || COALESCE(i.external_place_id, i.id::text)) ||
      '/cover.jpg',
    updated_at = now()
WHERE i.thumbnail_url LIKE 'https://cdn.soomgil.test/%'
   OR (i.thumbnail_url IS NULL AND i.trip_id::text LIKE 'c0000000-%');

UPDATE community.post_snapshot_items i
SET thumbnail_url = a.first_image1
FROM tourism_source.attractions a
WHERE i.place_provider = 'KTO' AND i.external_place_id = a.content_id::text
  AND (a.content_id BETWEEN 10001 AND 10040 OR a.content_id BETWEEN 20001 AND 20028);

UPDATE community.post_snapshot_items i
SET thumbnail_url = 'https://daobk0bynum21.cloudfront.net/demo/legacy-places/' ||
      md5(COALESCE(i.place_provider, 'CUSTOM') || ':' || COALESCE(i.external_place_id, i.id::text)) ||
      '/cover.jpg'
WHERE i.thumbnail_url LIKE 'https://cdn.soomgil.test/%';

-- Refresh the immutable-looking post payload after replacing itinerary and profile images.
UPDATE community.posts p
SET snapshot = jsonb_build_object(
  'days', COALESCE((
    SELECT jsonb_agg(jsonb_build_object(
      'id', d.id, 'tripId', d.trip_id, 'groupType', d.group_type,
      'dayNumber', d.day_number, 'date', d.date, 'title', d.title, 'sortOrder', d.sort_order,
      'items', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
          'id', i.id, 'itineraryDayId', i.itinerary_day_id, 'sortOrder', i.sort_order,
          'itemType', i.item_type,
          'placeRef', jsonb_build_object('provider', i.place_provider, 'externalPlaceId', i.external_place_id),
          'placeName', i.place_name, 'address', i.address, 'lat', i.lat, 'lng', i.lng,
          'thumbnailUrl', i.thumbnail_url, 'sourceStatus', i.source_status
        ) ORDER BY i.sort_order)
        FROM itinerary.itinerary_items i
        WHERE i.itinerary_day_id = d.id AND i.deleted_at IS NULL
      ), '[]'::jsonb)
    ) ORDER BY d.sort_order)
    FROM itinerary.itinerary_days d WHERE d.trip_id = p.source_trip_id
  ), '[]'::jsonb),
  'routes', COALESCE((
    SELECT jsonb_agg(jsonb_build_object(
      'id', r.id, 'originItineraryItemId', r.origin_itinerary_item_id,
      'destinationItineraryItemId', r.destination_itinerary_item_id,
      'mode', r.mode, 'provider', r.provider, 'providerProfile', r.provider_profile,
      'geometryFormat', r.geometry_format, 'geometry', r.geometry,
      'distanceMeters', r.distance_meters, 'durationSeconds', r.duration_seconds,
      'confidence', r.confidence
    ) ORDER BY r.created_at)
    FROM itinerary.trip_routes r WHERE r.trip_id = p.source_trip_id AND r.deleted_at IS NULL
  ), '[]'::jsonb),
  'authorDisplay', (
    SELECT jsonb_build_object('id', u.user_id, 'displayName', u.display_name,
      'profileImageUrl', u.profile_image_url)
    FROM auth.user_profiles u WHERE u.user_id = p.published_by_user_id
  )
)
WHERE p.id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR p.id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k));

UPDATE community.post_media pm
SET caption = CASE pm.sort_order
  WHEN 0 THEN p.title || '의 대표 장면'
  ELSE p.title || '에서 오래 기억에 남은 순간'
END
FROM community.posts p
WHERE pm.post_id = p.id
  AND (p.id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
       OR p.id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
         ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
         ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k)));

COMMIT;

SELECT 'demo_users' metric, count(*) value FROM auth.users
WHERE id IN (SELECT md5('demo-user:' || n)::uuid FROM generate_series(1, 120) n)
UNION ALL SELECT 'demo_posts', count(*) FROM community.posts
WHERE id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k))
UNION ALL SELECT 'demo_comments', count(*) FROM community.post_comments
WHERE post_id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR post_id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k))
UNION ALL SELECT 'demo_likes', count(*) FROM community.post_likes
WHERE post_id IN (SELECT md5('demo-bulk-post:' || g)::uuid FROM generate_series(1, 50) g)
   OR post_id IN (SELECT md5('demo-post:' || k)::uuid FROM (VALUES
     ('palace-post'), ('night-post'), ('seongsu-post'), ('science-post'), ('bread-post'),
     ('green-post'), ('modern-post'), ('family-post'), ('autumn-post')) v(k));
