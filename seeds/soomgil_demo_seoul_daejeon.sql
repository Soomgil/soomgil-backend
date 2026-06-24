-- Soomgil local demo dataset: Seoul + Daejeon
-- Target schema: Flyway V1..V37
-- Safe to re-run: deterministic identifiers and ON CONFLICT clauses are used throughout.
-- Apply after migrations:
--   docker compose exec -T postgres psql -U soomgil -d soomgil < seeds/soomgil_demo_seoul_daejeon.sql

BEGIN;

SET LOCAL TIME ZONE 'Asia/Seoul';

-- ---------------------------------------------------------------------------
-- Users: a small but socially connected local community
-- Demo password hash is intentionally non-production and shared by seed users.
-- ---------------------------------------------------------------------------
WITH seed(n, display_name, bio, image_seed, joined_days_ago) AS (
  VALUES
    (1, '서울산책러 민지', '퇴근 후 골목과 공원을 걷습니다. 조용한 카페와 노을을 좋아해요.', 'minji-walk', 420),
    (2, '대전토박이 준호', '대전의 빵, 과학, 오래된 동네를 기록합니다.', 'junho-daejeon', 390),
    (3, '주말여행자 수빈', '주말이면 카메라 하나 들고 가볍게 떠나요.', 'subin-weekend', 330),
    (4, '빵지순례 예린', '성심당부터 동네 빵집까지, 빵을 따라 여행합니다.', 'yerin-bread', 310),
    (5, '야경수집가 도윤', '도시의 불빛과 늦은 밤 산책 코스를 모으는 중.', 'doyun-night', 295),
    (6, '아이와여행 지우', '아이와 함께 배우고 쉬는 여행을 찾고 있어요.', 'jiwoo-family', 270),
    (7, '건축덕후 현우', '오래된 건물과 새로 태어난 공간을 좋아합니다.', 'hyunwoo-arch', 250),
    (8, '초록수집가 나연', '수목원, 숲, 강변이라면 어디든 좋아요.', 'nayeon-green', 225),
    (9, '맛있는지도 태호', '시장과 노포 중심으로 여행 동선을 짭니다.', 'taeho-food', 210),
    (10, '사진하는 유나', '빛이 예쁜 시간과 장소를 기록합니다.', 'yuna-photo', 198),
    (11, '느린여행 세진', '한 동네에 오래 머무는 여행을 선호해요.', 'sejin-slow', 176),
    (12, '과학소풍 은채', '과학관과 전시를 좋아하는 호기심 많은 여행자.', 'eunchae-science', 160),
    (13, '자전거탄 민석', '강변과 자전거길 위주로 여행합니다.', 'minseok-bike', 142),
    (14, '레트로소녀 하린', '오래된 간판, 골목, 다방 감성을 찾아다녀요.', 'harin-retro', 128),
    (15, '공원피크닉 우진', '돗자리와 커피만 있으면 어디든 여행지.', 'woojin-picnic', 112),
    (16, '전시보는 소연', '미술관과 독립서점이 있는 하루를 좋아합니다.', 'soyeon-art', 96),
    (17, '등산초보 건우', '무리하지 않는 전망 좋은 코스를 찾습니다.', 'geonwoo-hike', 80),
    (18, '커플여행 다은', '둘이 걷기 좋은 로맨틱한 코스를 모아요.', 'daeun-couple', 63),
    (19, '혼행러 재민', '혼자서도 편안한 교통 좋은 여행지를 소개해요.', 'jaemin-solo', 48),
    (20, '숨길 운영자', '숨길 데모 커뮤니티를 관리합니다.', 'soomgil-admin', 500)
)
INSERT INTO auth.users (id, status, status_changed_at, last_login_at, created_at, updated_at)
SELECT md5('demo-user:' || n)::uuid, 'ACTIVE', now() - make_interval(days => joined_days_ago),
       now() - make_interval(hours => (n * 7) % 120),
       now() - make_interval(days => joined_days_ago), now() - make_interval(hours => n % 48)
FROM seed
ON CONFLICT (id) DO NOTHING;

WITH seed(n, display_name, bio, image_seed) AS (
  VALUES
    (1,'서울산책러 민지','퇴근 후 골목과 공원을 걷습니다. 조용한 카페와 노을을 좋아해요.','minji-walk'),
    (2,'대전토박이 준호','대전의 빵, 과학, 오래된 동네를 기록합니다.','junho-daejeon'),
    (3,'주말여행자 수빈','주말이면 카메라 하나 들고 가볍게 떠나요.','subin-weekend'),
    (4,'빵지순례 예린','성심당부터 동네 빵집까지, 빵을 따라 여행합니다.','yerin-bread'),
    (5,'야경수집가 도윤','도시의 불빛과 늦은 밤 산책 코스를 모으는 중.','doyun-night'),
    (6,'아이와여행 지우','아이와 함께 배우고 쉬는 여행을 찾고 있어요.','jiwoo-family'),
    (7,'건축덕후 현우','오래된 건물과 새로 태어난 공간을 좋아합니다.','hyunwoo-arch'),
    (8,'초록수집가 나연','수목원, 숲, 강변이라면 어디든 좋아요.','nayeon-green'),
    (9,'맛있는지도 태호','시장과 노포 중심으로 여행 동선을 짭니다.','taeho-food'),
    (10,'사진하는 유나','빛이 예쁜 시간과 장소를 기록합니다.','yuna-photo'),
    (11,'느린여행 세진','한 동네에 오래 머무는 여행을 선호해요.','sejin-slow'),
    (12,'과학소풍 은채','과학관과 전시를 좋아하는 호기심 많은 여행자.','eunchae-science'),
    (13,'자전거탄 민석','강변과 자전거길 위주로 여행합니다.','minseok-bike'),
    (14,'레트로소녀 하린','오래된 간판, 골목, 다방 감성을 찾아다녀요.','harin-retro'),
    (15,'공원피크닉 우진','돗자리와 커피만 있으면 어디든 여행지.','woojin-picnic'),
    (16,'전시보는 소연','미술관과 독립서점이 있는 하루를 좋아합니다.','soyeon-art'),
    (17,'등산초보 건우','무리하지 않는 전망 좋은 코스를 찾습니다.','geonwoo-hike'),
    (18,'커플여행 다은','둘이 걷기 좋은 로맨틱한 코스를 모아요.','daeun-couple'),
    (19,'혼행러 재민','혼자서도 편안한 교통 좋은 여행지를 소개해요.','jaemin-solo'),
    (20,'숨길 운영자','숨길 데모 커뮤니티를 관리합니다.','soomgil-admin')
)
INSERT INTO auth.user_profiles (user_id, display_name, profile_image_url, bio, profile_visibility, created_at, updated_at)
SELECT md5('demo-user:' || n)::uuid, display_name,
       'https://i.pravatar.cc/300?u=soomgil-' || image_seed,
       bio, 'PUBLIC', now() - make_interval(days => 500 - n * 9), now() - make_interval(hours => n)
FROM seed
ON CONFLICT (user_id) DO UPDATE SET display_name = EXCLUDED.display_name, bio = EXCLUDED.bio,
  profile_image_url = EXCLUDED.profile_image_url, updated_at = EXCLUDED.updated_at;

INSERT INTO auth.user_email_addresses
  (id, user_id, email, normalized_email, is_primary, verified_at, created_at, updated_at)
SELECT md5('demo-email:' || n)::uuid, md5('demo-user:' || n)::uuid,
       'demo' || lpad(n::text, 2, '0') || '@soomgil.local',
       'demo' || lpad(n::text, 2, '0') || '@soomgil.local', true,
       now() - make_interval(days => 200 - n), now() - make_interval(days => 220 - n), now()
FROM generate_series(1, 20) n
ON CONFLICT DO NOTHING;

INSERT INTO auth.user_settings (user_id, display_language, timezone, marketing_email_opt_in,
  marketing_email_opted_in_at, trip_invite_email_opt_in, created_at, updated_at)
SELECT md5('demo-user:' || n)::uuid, 'ko', 'Asia/Seoul', n % 3 = 0,
       CASE WHEN n % 3 = 0 THEN now() - make_interval(days => n) END, true,
       now() - make_interval(days => 240 - n), now()
FROM generate_series(1, 20) n
ON CONFLICT (user_id) DO NOTHING;

-- Shared local credential for visual/demo accounts only: Soomgil123!
INSERT INTO auth.user_password_credentials
  (user_id, password_hash, password_algorithm, password_changed_at, created_at, updated_at)
SELECT md5('demo-user:' || n)::uuid,
       '$2a$10$4zIe8rBz3.2nGyj4JC/8uug1K82T9xDq74iVLGlgN1b5hzz5YEpCe',
       'bcrypt', now() - interval '90 days', now() - interval '90 days', now()
FROM generate_series(1, 20) n
ON CONFLICT (user_id) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  password_algorithm = EXCLUDED.password_algorithm,
  updated_at = EXCLUDED.updated_at;

INSERT INTO auth.user_policy_acceptances
  (id, user_id, policy_document_id, acceptance_method, accepted_at, created_at)
SELECT md5('demo-policy:' || n || ':' || p.id)::uuid, md5('demo-user:' || n)::uuid,
       p.id, 'EXPLICIT', now() - make_interval(days => 200 - n), now() - make_interval(days => 200 - n)
FROM generate_series(1, 20) n
CROSS JOIN auth.policy_documents p
WHERE p.id IN ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id, granted_by_user_id, granted_at)
SELECT md5('demo-user:20')::uuid, id, md5('demo-user:20')::uuid, now() - interval '300 days'
FROM auth.roles WHERE code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Region masters used by trip creation and place filtering
-- ---------------------------------------------------------------------------
INSERT INTO geo.legal_regions
  (code, name, full_name, level, parent_code, sido_code, sigungu_code, raw_status, is_active, synced_at)
VALUES
  ('1100000000','서울특별시','서울특별시','SIDO',NULL,'11',NULL,'존재',true,now()),
  ('1111000000','종로구','서울특별시 종로구','SIGUNGU','1100000000','11','11110','존재',true,now()),
  ('1114000000','중구','서울특별시 중구','SIGUNGU','1100000000','11','11140','존재',true,now()),
  ('1117000000','용산구','서울특별시 용산구','SIGUNGU','1100000000','11','11170','존재',true,now()),
  ('1120000000','성동구','서울특별시 성동구','SIGUNGU','1100000000','11','11200','존재',true,now()),
  ('1144000000','마포구','서울특별시 마포구','SIGUNGU','1100000000','11','11440','존재',true,now()),
  ('1171000000','송파구','서울특별시 송파구','SIGUNGU','1100000000','11','11710','존재',true,now()),
  ('3000000000','대전광역시','대전광역시','SIDO',NULL,'30',NULL,'존재',true,now()),
  ('3011000000','동구','대전광역시 동구','SIGUNGU','3000000000','30','30110','존재',true,now()),
  ('3014000000','중구','대전광역시 중구','SIGUNGU','3000000000','30','30140','존재',true,now()),
  ('3017000000','서구','대전광역시 서구','SIGUNGU','3000000000','30','30170','존재',true,now()),
  ('3020000000','유성구','대전광역시 유성구','SIGUNGU','3000000000','30','30200','존재',true,now()),
  ('3023000000','대덕구','대전광역시 대덕구','SIGUNGU','3000000000','30','30230','존재',true,now())
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name, full_name=EXCLUDED.full_name,
  is_active=true, synced_at=EXCLUDED.synced_at;

INSERT INTO tourism_source.sidos (sido_code, sido_name) VALUES (1,'서울'),(3,'대전')
ON CONFLICT (sido_code) DO UPDATE SET sido_name=EXCLUDED.sido_name;

INSERT INTO tourism_source.guguns (sido_code, gugun_code, gugun_name) VALUES
  (1,1,'강남구'),(1,14,'마포구'),(1,23,'종로구'),(1,24,'중구'),(1,21,'용산구'),
  (1,16,'성동구'),(1,18,'송파구'),(3,1,'대덕구'),(3,2,'동구'),(3,3,'서구'),(3,4,'유성구'),(3,5,'중구')
ON CONFLICT (sido_code,gugun_code) DO UPDATE SET gugun_name=EXCLUDED.gugun_name;

INSERT INTO tourism_source.contenttypes (content_type_id, content_type_name) VALUES
  (12,'관광지'),(14,'문화시설'),(15,'축제/공연/행사'),(25,'여행코스'),(28,'레포츠'),(32,'숙박'),(38,'쇼핑'),(39,'음식점')
ON CONFLICT (content_type_id) DO UPDATE SET content_type_name=EXCLUDED.content_type_name;

-- ---------------------------------------------------------------------------
-- Searchable real-world places. Image endpoints are deterministic demo assets.
-- ---------------------------------------------------------------------------
WITH place(content_id,title,content_type_id,area_code,gugun,lat,lng,tel,addr,homepage,overview,seed) AS (
  VALUES
    (10001,'경복궁',12,1,23,37.579617,126.977041,'02-3700-3900','서울특별시 종로구 사직로 161','https://royal.khs.go.kr/','서울을 대표하는 조선 왕조의 법궁. 넓은 전각과 근정전, 향원정 산책이 좋다.','gyeongbokgung'),
    (10002,'북촌한옥마을',12,1,23,37.582604,126.983100,NULL,'서울특별시 종로구 계동길 37',NULL,'한옥과 골목 풍경이 이어지는 서울의 대표적인 전통 주거 지역.','bukchon'),
    (10003,'익선동 한옥거리',12,1,23,37.574340,126.989720,NULL,'서울특별시 종로구 수표로28길 일대',NULL,'작은 한옥 카페와 식당, 소품점이 모여 있는 도심 골목.','ikseondong'),
    (10004,'청계천',12,1,24,37.569030,126.978600,'02-2290-7111','서울특별시 종로구 창신동','https://www.sisul.or.kr/open_content/cheonggye/','서울 도심을 가로지르는 수변 산책로. 야간 조명이 아름답다.','cheonggyecheon'),
    (10005,'덕수궁 돌담길',12,1,24,37.565530,126.975160,NULL,'서울특별시 중구 세종대로19길 24',NULL,'덕수궁 담장을 따라 걷는 서울의 고즈넉한 산책길.','deoksugung'),
    (10006,'동대문디자인플라자 DDP',14,1,24,37.566480,127.009200,'02-2153-0000','서울특별시 중구 을지로 281','https://ddp.or.kr/','전시와 디자인, 야경을 함께 즐기는 유선형 건축 랜드마크.','ddp'),
    (10007,'N서울타워',12,1,21,37.551170,126.988220,'02-3455-9277','서울특별시 용산구 남산공원길 105','https://www.seoultower.co.kr/','서울 전경을 한눈에 볼 수 있는 남산의 전망 명소.','nseoultower'),
    (10008,'국립중앙박물관',14,1,21,37.523850,126.980470,'02-2077-9000','서울특별시 용산구 서빙고로 137','https://www.museum.go.kr/','한국의 역사와 문화를 시대별로 만나는 대형 국립 박물관.','nationalmuseum'),
    (10009,'노들섬',14,1,21,37.517790,126.958980,NULL,'서울특별시 용산구 양녕로 445','https://nodeul.org/','한강 한가운데에서 공연, 전시, 피크닉과 노을을 즐기는 복합문화공간.','nodeulisland'),
    (10010,'서울숲',12,1,16,37.544390,127.037440,'02-460-2905','서울특별시 성동구 뚝섬로 273','https://seoulforest.or.kr/','산책과 피크닉, 자전거를 즐기기 좋은 도심 속 대형 공원.','seoulforest'),
    (10011,'성수동 카페거리',12,1,16,37.544570,127.055960,NULL,'서울특별시 성동구 성수이로 일대',NULL,'낡은 공장과 창고가 카페와 편집숍으로 재생된 감각적인 거리.','seongsu'),
    (10012,'망원시장',38,1,14,37.556770,126.905650,'02-335-3591','서울특별시 마포구 포은로8길 14',NULL,'동네 먹거리와 장보기 문화를 함께 느끼는 활기찬 전통시장.','mangwonmarket'),
    (10013,'문화비축기지',14,1,14,37.571010,126.894790,'02-376-8410','서울특별시 마포구 증산로 87','https://parks.seoul.go.kr/template/sub/culturetank.do','석유 탱크를 공연장과 전시공간으로 바꾼 산업유산 재생공원.','culturetank'),
    (10014,'하늘공원',12,1,14,37.567450,126.885260,NULL,'서울특별시 마포구 하늘공원로 95',NULL,'억새와 노을, 탁 트인 서울 풍경으로 사랑받는 월드컵공원 전망 명소.','haneulpark'),
    (10015,'롯데월드타워 서울스카이',12,1,18,37.512570,127.102540,'1661-2000','서울특별시 송파구 올림픽로 300','https://seoulsky.lotteworld.com/','서울의 스카이라인을 내려다보는 초고층 전망대.','seoulsky'),
    (10016,'올림픽공원',12,1,18,37.520880,127.121200,'02-410-1114','서울특별시 송파구 올림픽로 424','https://www.ksponco.or.kr/olympicpark/','넓은 잔디와 조각 작품, 몽촌토성이 어우러진 산책 명소.','olympicpark'),
    (10017,'반포한강공원',12,1,1,37.510360,126.995930,NULL,'서울특별시 서초구 신반포로11길 40',NULL,'달빛무지개분수와 야경, 피크닉을 즐기는 한강 공원.','banpohangang'),
    (10018,'낙산공원',12,1,23,37.580660,127.007590,'02-743-7985','서울특별시 종로구 낙산길 41',NULL,'성곽길을 따라 서울 도심 야경을 감상하기 좋은 공원.','naksanpark'),
    (20001,'국립중앙과학관',14,3,4,36.375500,127.377700,'042-601-7979','대전광역시 유성구 대덕대로 481','https://www.science.go.kr/','과학 원리와 자연사를 체험형 전시로 만나는 대전 대표 과학관.','science-museum'),
    (20002,'엑스포과학공원',12,3,4,36.376200,127.388100,NULL,'대전광역시 유성구 대덕대로 480',NULL,'한빛탑과 엑스포다리 야경이 인상적인 대전의 상징적 공간.','expo-park'),
    (20003,'한밭수목원',12,3,3,36.366800,127.388500,'042-270-8452','대전광역시 서구 둔산대로 169','https://www.daejeon.go.kr/gar/','도심에서 계절 식물과 넓은 잔디를 만나는 중부권 최대 도심 수목원.','hanbat-arboretum'),
    (20004,'대전시립미술관',14,3,3,36.366400,127.385800,'042-270-7370','대전광역시 서구 둔산대로 155','https://www.daejeon.go.kr/dma/','한밭수목원과 함께 둘러보기 좋은 현대미술 전시공간.','daejeon-museum-art'),
    (20005,'유성온천 족욕체험장',12,3,4,36.355350,127.341800,NULL,'대전광역시 유성구 봉명동 574',NULL,'도심 속에서 무료로 온천 족욕을 즐기는 유성의 휴식 공간.','yuseong-footbath'),
    (20006,'성심당 본점',39,3,5,36.327680,127.427320,'1588-8069','대전광역시 중구 대종로480번길 15','https://www.sungsimdang.co.kr/','튀김소보로와 부추빵으로 유명한 대전 대표 로컬 베이커리.','sungsimdang'),
    (20007,'소제동 카페거리',12,3,2,36.335500,127.438200,NULL,'대전광역시 동구 수향길 일대',NULL,'철도관사촌의 오래된 집을 개조한 카페와 식당이 이어지는 골목.','soje-dong'),
    (20008,'우암사적공원',12,3,2,36.347250,127.457650,'042-673-9286','대전광역시 동구 충정로 53',NULL,'조선 유학자 우암 송시열의 흔적과 고즈넉한 전통 건축을 만나는 공원.','uam-park'),
    (20009,'장태산자연휴양림',12,3,3,36.216900,127.341900,'042-270-7883','대전광역시 서구 장안로 461','https://www.jangtaesan.or.kr/','메타세쿼이아 숲과 스카이웨이가 유명한 대전의 대표 자연휴양림.','jangtaesan'),
    (20010,'계족산 황톳길',28,3,1,36.405800,127.451900,NULL,'대전광역시 대덕구 장동 453-1',NULL,'맨발로 걷는 붉은 황톳길과 숲길로 유명한 치유 산책 코스.','gyejoksan'),
    (20011,'대청호 오백리길',28,3,1,36.447100,127.473300,NULL,'대전광역시 대덕구 대청로 일대',NULL,'대청호의 잔잔한 물빛과 숲을 따라 걷는 장거리 생태길.','daecheongho'),
    (20012,'대전근현대사전시관',14,3,5,36.327000,127.421900,'042-270-4537','대전광역시 중구 중앙로 101',NULL,'옛 충남도청 본관에서 대전의 근현대사를 살펴보는 전시관.','modern-history'),
    (20013,'대전오월드',12,3,5,36.288800,127.397900,'042-580-4820','대전광역시 중구 사정공원로 70','https://www.oworld.kr/','동물원, 플라워랜드, 놀이시설을 함께 즐기는 가족 테마공원.','oworld'),
    (20014,'테미오래',14,3,5,36.322900,127.421100,'042-335-5701','대전광역시 중구 보문로205번길 13',NULL,'옛 충남도지사 관사를 문화예술 공간으로 재생한 근대건축 마을.','temiorae')
)
INSERT INTO tourism_source.attractions
  (content_id,title,content_type_id,area_code,si_gun_gu_code,first_image1,first_image2,map_level,
   latitude,longitude,tel,addr1,homepage,overview,source_hash,source_modified_at,imported_at)
SELECT content_id,title,content_type_id,area_code,gugun,
       'https://picsum.photos/seed/soomgil-' || seed || '/1200/800',
       'https://picsum.photos/seed/soomgil-' || seed || '-detail/800/600', 6,
       lat,lng,tel,addr,homepage,overview,md5(title || ':' || addr),now(),now()
FROM place
ON CONFLICT (content_id) DO UPDATE SET title=EXCLUDED.title, content_type_id=EXCLUDED.content_type_id,
  area_code=EXCLUDED.area_code, si_gun_gu_code=EXCLUDED.si_gun_gu_code,
  first_image1=EXCLUDED.first_image1, first_image2=EXCLUDED.first_image2,
  latitude=EXCLUDED.latitude, longitude=EXCLUDED.longitude, tel=EXCLUDED.tel,
  addr1=EXCLUDED.addr1, homepage=EXCLUDED.homepage, overview=EXCLUDED.overview,
  source_hash=EXCLUDED.source_hash, source_modified_at=EXCLUDED.source_modified_at;

INSERT INTO tourism_source.attraction_images
  (id,attraction_no,source_provider,source_type,original_url,public_url,display_order,width,height,is_active)
SELECT md5('demo-attraction-image:' || a.content_id)::uuid, a.no, 'DEMO','PRIMARY',a.first_image1,
       a.first_image1,0,1200,800,true
FROM tourism_source.attractions a
WHERE a.content_id BETWEEN 10001 AND 20014
ON CONFLICT (id) DO UPDATE SET public_url=EXCLUDED.public_url,is_active=true;

-- Place enrichment gives the swipe/recommendation screens meaningful reasons.
INSERT INTO preference.place_tag_enrichments
  (id,provider,external_place_id,source_hash,status,model_provider,model_name,prompt_version,
   tag_dictionary_version,selection_policy_version,candidate_count,selected_count,enriched_at)
SELECT md5('demo-enrichment:' || content_id)::uuid,'KTO',content_id::text,source_hash,'SUCCEEDED',
       'CURATED','soomgil-demo-curator','demo-v1','preference-tags-v1','demo-policy-v1',4,4,now()
FROM tourism_source.attractions WHERE content_id BETWEEN 10001 AND 20014
ON CONFLICT (id) DO UPDATE SET status='SUCCEEDED',selected_count=4,enriched_at=now();

WITH mapped(content_id, tag_code, rank_order, confidence, weight) AS (
  VALUES
    (10001,'history',1,.98,1.20),(10001,'palace_fortress',2,.97,1.18),(10001,'traditional_architecture',3,.94,1.10),(10001,'photo_spot',4,.85,.90),
    (10002,'traditional',1,.96,1.15),(10002,'street_alley',2,.94,1.10),(10002,'photo_spot',3,.92,1.05),(10002,'walking',4,.84,.90),
    (10003,'street_alley',1,.96,1.10),(10003,'nostalgic',2,.91,1.05),(10003,'lively',3,.85,.88),(10003,'photo_spot',4,.83,.86),
    (10004,'waterfront',1,.96,1.14),(10004,'walking',2,.95,1.10),(10004,'urban',3,.88,.95),(10004,'night_view',4,.87,.96),
    (10005,'walking',1,.95,1.10),(10005,'history',2,.90,1.00),(10005,'romantic',3,.86,.94),(10005,'quiet',4,.82,.88),
    (10006,'architecture',1,.98,1.20),(10006,'modern',2,.95,1.12),(10006,'night_view',3,.91,1.04),(10006,'gallery_exhibition',4,.88,.96),
    (10007,'observatory',1,.99,1.25),(10007,'night_view',2,.98,1.22),(10007,'landmark',3,.97,1.18),(10007,'romantic',4,.87,.94),
    (10008,'museum',1,.99,1.20),(10008,'history',2,.96,1.15),(10008,'educational',3,.94,1.10),(10008,'indoor',4,.90,1.00),
    (10009,'waterfront',1,.93,1.05),(10009,'sunset',2,.92,1.08),(10009,'picnic',3,.91,1.02),(10009,'cultural_space',4,.87,.93),
    (10010,'park',1,.99,1.18),(10010,'nature_escape',2,.97,1.15),(10010,'picnic',3,.94,1.08),(10010,'walking',4,.93,1.06),
    (10011,'industrial_heritage',1,.91,1.05),(10011,'modern',2,.90,1.02),(10011,'street_alley',3,.88,.96),(10011,'lively',4,.86,.92),
    (10012,'local_culture',1,.94,1.10),(10012,'lively',2,.91,1.04),(10012,'street_alley',3,.83,.90),(10012,'unique',4,.81,.86),
    (10013,'industrial_heritage',1,.99,1.22),(10013,'architecture',2,.92,1.08),(10013,'cultural_space',3,.91,1.05),(10013,'unique',4,.88,.98),
    (10014,'scenic_view',1,.97,1.16),(10014,'sunset',2,.96,1.15),(10014,'walking',3,.91,1.02),(10014,'open_feeling',4,.90,1.00),
    (10015,'observatory',1,.99,1.24),(10015,'landmark',2,.98,1.20),(10015,'night_view',3,.95,1.14),(10015,'modern',4,.90,1.00),
    (10016,'park',1,.98,1.16),(10016,'walking',2,.96,1.12),(10016,'picnic',3,.94,1.08),(10016,'history',4,.80,.85),
    (10017,'waterfront',1,.98,1.18),(10017,'night_view',2,.95,1.13),(10017,'picnic',3,.94,1.08),(10017,'romantic',4,.90,1.00),
    (10018,'night_view',1,.96,1.16),(10018,'walking',2,.94,1.10),(10018,'history',3,.89,1.00),(10018,'scenic_view',4,.88,.98),
    (20001,'science_education',1,.99,1.25),(20001,'hands_on_experience',2,.95,1.14),(20001,'educational',3,.94,1.10),(20001,'indoor',4,.90,1.00),
    (20002,'landmark',1,.96,1.16),(20002,'night_view',2,.94,1.12),(20002,'science_education',3,.89,1.00),(20002,'walking',4,.84,.90),
    (20003,'arboretum',1,.99,1.22),(20003,'garden',2,.96,1.14),(20003,'walking',3,.94,1.10),(20003,'healing',4,.92,1.06),
    (20004,'gallery_exhibition',1,.98,1.19),(20004,'artistic',2,.95,1.13),(20004,'indoor',3,.91,1.02),(20004,'quiet',4,.85,.92),
    (20005,'hot_spring',1,.99,1.24),(20005,'healing',2,.97,1.16),(20005,'unique',3,.88,.96),(20005,'urban',4,.80,.85),
    (20006,'local_culture',1,.98,1.18),(20006,'lively',2,.90,1.02),(20006,'landmark',3,.86,.94),(20006,'unique',4,.82,.88),
    (20007,'nostalgic',1,.96,1.15),(20007,'street_alley',2,.95,1.12),(20007,'industrial_heritage',3,.90,1.01),(20007,'photo_spot',4,.89,1.00),
    (20008,'traditional_architecture',1,.95,1.12),(20008,'history',2,.94,1.10),(20008,'quiet',3,.90,1.01),(20008,'park',4,.84,.90),
    (20009,'forest',1,.99,1.25),(20009,'walking',2,.97,1.16),(20009,'healing',3,.96,1.14),(20009,'scenic_view',4,.92,1.05),
    (20010,'walking',1,.98,1.20),(20010,'forest',2,.96,1.15),(20010,'healing',3,.92,1.06),(20010,'hiking',4,.88,.98),
    (20011,'lake_pond',1,.98,1.20),(20011,'waterfront',2,.96,1.15),(20011,'walking',3,.92,1.06),(20011,'scenic_view',4,.90,1.02),
    (20012,'history',1,.96,1.15),(20012,'museum',2,.94,1.10),(20012,'architecture',3,.91,1.04),(20012,'indoor',4,.84,.90),
    (20013,'theme_park',1,.98,1.20),(20013,'animal_viewing',2,.95,1.14),(20013,'rides',3,.94,1.10),(20013,'hands_on_experience',4,.88,.96),
    (20014,'architecture',1,.95,1.13),(20014,'nostalgic',2,.93,1.08),(20014,'cultural_space',3,.91,1.04),(20014,'photo_spot',4,.89,1.00)
)
INSERT INTO preference.place_tag_enrichment_tags
  (enrichment_id,tag_id,confidence,weight,selection_score,rank_order,rationale)
SELECT md5('demo-enrichment:' || m.content_id)::uuid,t.id,m.confidence,m.weight,
       round((m.confidence * m.weight)::numeric,6),m.rank_order,'서울·대전 데모 장소 큐레이션'
FROM mapped m JOIN preference.preference_tags t ON t.code=m.tag_code
ON CONFLICT (enrichment_id,tag_id) DO UPDATE SET confidence=EXCLUDED.confidence,
  weight=EXCLUDED.weight,selection_score=EXCLUDED.selection_score,rank_order=EXCLUDED.rank_order;

-- ---------------------------------------------------------------------------
-- Preference history: enough reactions for personalized feeds and search cards
-- ---------------------------------------------------------------------------
INSERT INTO preference.user_place_reactions
  (id,user_id,provider,external_place_id,reaction,reaction_count,first_reacted_at,last_reacted_at,place_tag_enrichment_id)
SELECT md5('demo-reaction:' || u || ':' || a.content_id)::uuid, md5('demo-user:' || u)::uuid,
       'KTO',a.content_id::text,
       CASE WHEN (u + a.content_id) % 11 = 0 THEN 'NOPE'
            WHEN (u * 3 + a.content_id) % 7 = 0 THEN 'SUPER_LIKE' ELSE 'LIKE' END,
       1,now() - make_interval(days => ((u * 5 + a.content_id) % 80)),
       now() - make_interval(hours => ((u * 11 + a.content_id) % 240)),
       md5('demo-enrichment:' || a.content_id)::uuid
FROM generate_series(1,20) u
CROSS JOIN tourism_source.attractions a
WHERE a.content_id BETWEEN 10001 AND 20014 AND (u * 13 + a.content_id) % 4 <> 0
ON CONFLICT (user_id,provider,external_place_id) DO NOTHING;

INSERT INTO preference.user_swipe_events
  (user_id,provider,external_place_id,reaction,feed_context,place_tag_enrichment_id,occurred_at)
SELECT r.user_id,r.provider,r.external_place_id,r.reaction,
       jsonb_build_object('region',CASE WHEN r.external_place_id::int < 20000 THEN '서울' ELSE '대전' END,
                          'surface','SWIPE','seed','seoul-daejeon-demo'),
       r.place_tag_enrichment_id,r.last_reacted_at
FROM preference.user_place_reactions r
WHERE r.id::text IS NOT NULL AND r.place_tag_enrichment_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM preference.user_swipe_events e
                  WHERE e.user_id=r.user_id AND e.provider=r.provider
                    AND e.external_place_id=r.external_place_id AND e.occurred_at=r.last_reacted_at);

INSERT INTO preference.user_saved_places (id,user_id,provider,external_place_id,created_at)
SELECT md5('demo-save:' || r.user_id || ':' || r.provider || ':' || r.external_place_id)::uuid,
       r.user_id,r.provider,r.external_place_id,r.last_reacted_at
FROM preference.user_place_reactions r
WHERE r.user_id IN (SELECT md5('demo-user:' || u)::uuid FROM generate_series(1,20) u)
  AND r.reaction = 'SUPER_LIKE'
ON CONFLICT (user_id,provider,external_place_id) DO NOTHING;

INSERT INTO preference.user_preference_tag_weights
  (user_id,tag_id,positive_evidence,negative_evidence,preference_score,like_count,super_like_count,nope_count,updated_at)
SELECT r.user_id,t.tag_id,
       sum(CASE r.reaction WHEN 'LIKE' THEN t.weight WHEN 'SUPER_LIKE' THEN t.weight*1.7 ELSE 0 END),
       sum(CASE r.reaction WHEN 'NOPE' THEN t.weight ELSE 0 END),
       greatest(0.05,least(0.95,0.5 +
         (sum(CASE WHEN r.reaction IN ('LIKE','SUPER_LIKE') THEN t.weight ELSE 0 END)
          - sum(CASE WHEN r.reaction='NOPE' THEN t.weight ELSE 0 END)) / 20.0)),
       count(*) FILTER (WHERE r.reaction='LIKE'),count(*) FILTER (WHERE r.reaction='SUPER_LIKE'),
       count(*) FILTER (WHERE r.reaction='NOPE'),now()
FROM preference.user_place_reactions r
JOIN preference.place_tag_enrichment_tags t ON t.enrichment_id=r.place_tag_enrichment_id
WHERE r.place_tag_enrichment_id::text LIKE '%'
GROUP BY r.user_id,t.tag_id
ON CONFLICT (user_id,tag_id) DO UPDATE SET positive_evidence=EXCLUDED.positive_evidence,
  negative_evidence=EXCLUDED.negative_evidence,preference_score=EXCLUDED.preference_score,
  like_count=EXCLUDED.like_count,super_like_count=EXCLUDED.super_like_count,
  nope_count=EXCLUDED.nope_count,updated_at=now();

-- ---------------------------------------------------------------------------
-- Trips, members and schedules
-- ---------------------------------------------------------------------------
WITH trips(k,owner,title,destination,status,version,created_days,updated_hours) AS (
  VALUES
    ('seoul-palace',1,'서울 궁궐과 오래된 골목 2박 3일','서울 종로·중구','ACTIVE',12,145,36),
    ('seoul-night',5,'한강부터 성곽까지 서울 야경 수집','서울 용산·종로·송파','ACTIVE',8,105,18),
    ('seongsu-picnic',3,'성수와 서울숲 느린 주말','서울 성동구','ACTIVE',6,82,12),
    ('daejeon-science',12,'아이처럼 즐기는 대전 과학 소풍','대전 유성·서구','ACTIVE',10,120,30),
    ('daejeon-bread',4,'빵과 골목을 따라 걷는 대전 1박 2일','대전 중구·동구','ACTIVE',9,98,20),
    ('daejeon-green',8,'장태산과 대청호 초록 충전','대전 서구·대덕구','ACTIVE',7,75,10),
    ('family-daejeon',6,'아이와 대전 체험 여행','대전 유성·중구','ACTIVE',5,44,7),
    ('seoul-modern',7,'서울 재생건축 탐방','서울 중구·성동·마포','ACTIVE',11,130,14),
    ('seoul-summer',18,'여름밤 서울 데이트 계획','서울 용산·송파','ACTIVE',3,12,2),
    ('daejeon-autumn',17,'가을 계족산과 온천 여행','대전 대덕·유성','ACTIVE',2,8,1)
)
INSERT INTO trip.trips (id,owner_user_id,title,display_destination,status,itinerary_version,created_at,updated_at)
SELECT md5('demo-trip:'||k)::uuid,md5('demo-user:'||owner)::uuid,title,destination,status,version,
       now()-make_interval(days=>created_days),now()-make_interval(hours=>updated_hours)
FROM trips ON CONFLICT (id) DO UPDATE SET title=EXCLUDED.title,display_destination=EXCLUDED.display_destination,
  status=EXCLUDED.status,itinerary_version=EXCLUDED.itinerary_version,updated_at=EXCLUDED.updated_at;

WITH trip_region(k,code,sort_order) AS (
  VALUES
   ('seoul-palace','1100000000',0),('seoul-night','1100000000',0),('seongsu-picnic','1100000000',0),
   ('seoul-modern','1100000000',0),('seoul-summer','1100000000',0),
   ('daejeon-science','3000000000',0),('daejeon-bread','3000000000',0),('daejeon-green','3000000000',0),
   ('family-daejeon','3000000000',0),('daejeon-autumn','3000000000',0)
)
INSERT INTO trip.trip_regions (trip_id,legal_region_code,sort_order,created_at)
SELECT md5('demo-trip:'||k)::uuid,code,sort_order,now()-interval '60 days' FROM trip_region
ON CONFLICT DO NOTHING;

WITH membership(k,u,joined_days) AS (
  VALUES
    ('seoul-palace',1,145),('seoul-palace',3,142),('seoul-palace',7,140),('seoul-palace',10,138),
    ('seoul-night',5,105),('seoul-night',10,102),('seoul-night',18,100),
    ('seongsu-picnic',3,82),('seongsu-picnic',8,80),('seongsu-picnic',15,78),('seongsu-picnic',16,76),
    ('daejeon-science',12,120),('daejeon-science',2,117),('daejeon-science',6,115),
    ('daejeon-bread',4,98),('daejeon-bread',2,96),('daejeon-bread',9,94),('daejeon-bread',14,93),
    ('daejeon-green',8,75),('daejeon-green',11,72),('daejeon-green',13,70),('daejeon-green',17,68),
    ('family-daejeon',6,44),('family-daejeon',12,42),('family-daejeon',15,40),
    ('seoul-modern',7,130),('seoul-modern',11,127),('seoul-modern',14,125),('seoul-modern',16,123),
    ('seoul-summer',18,12),('seoul-summer',5,10),
    ('daejeon-autumn',17,8),('daejeon-autumn',2,7),('daejeon-autumn',8,6)
)
INSERT INTO trip.trip_members (id,trip_id,user_id,role,status,joined_at)
SELECT md5('demo-member:'||k||':'||u)::uuid,md5('demo-trip:'||k)::uuid,md5('demo-user:'||u)::uuid,
       'MEMBER','ACTIVE',now()-make_interval(days=>joined_days)
FROM membership ON CONFLICT (trip_id,user_id) DO NOTHING;

WITH day_seed(k,d,trip_date,title) AS (
  VALUES
    ('seoul-palace',1,'2026-03-14'::date,'궁궐과 북촌'),('seoul-palace',2,'2026-03-15','익선동과 도심 산책'),('seoul-palace',3,'2026-03-16','남산과 박물관'),
    ('seoul-night',1,'2026-04-11','노을에서 야경까지'),('seoul-night',2,'2026-04-12','서울의 높은 곳'),
    ('seongsu-picnic',1,'2026-05-02','서울숲 피크닉'),('seongsu-picnic',2,'2026-05-03','성수 공간 탐방'),
    ('daejeon-science',1,'2026-03-28','과학과 전시'),('daejeon-science',2,'2026-03-29','수목원과 온천'),
    ('daejeon-bread',1,'2026-04-18','원도심 빵지순례'),('daejeon-bread',2,'2026-04-19','소제동과 전통공원'),
    ('daejeon-green',1,'2026-05-16','장태산 숲길'),('daejeon-green',2,'2026-05-17','대청호와 계족산'),
    ('family-daejeon',1,'2026-07-25','과학 체험'),('family-daejeon',2,'2026-07-26','동물과 놀이'),
    ('seoul-modern',1,'2026-02-21','DDP와 도심 건축'),('seoul-modern',2,'2026-02-22','성수와 문화비축기지'),
    ('seoul-summer',1,'2026-07-11','한강의 여름밤'),('seoul-summer',2,'2026-07-12','송파 야경'),
    ('daejeon-autumn',1,'2026-10-17','계족산 황톳길'),('daejeon-autumn',2,'2026-10-18','유성 온천 휴식')
)
INSERT INTO itinerary.itinerary_days (id,trip_id,group_type,day_number,date,title,sort_order,created_at,updated_at)
SELECT md5('demo-day:'||k||':'||d)::uuid,md5('demo-trip:'||k)::uuid,'DAY',d,trip_date,title,d-1,
       now()-interval '70 days',now()-interval '5 days'
FROM day_seed ON CONFLICT (id) DO UPDATE SET date=EXCLUDED.date,title=EXCLUDED.title,sort_order=EXCLUDED.sort_order;

WITH item_seed(k,d,s,content_id,creator) AS (
  VALUES
    ('seoul-palace',1,0,10001,1),('seoul-palace',1,1,10002,3),('seoul-palace',1,2,10005,10),
    ('seoul-palace',2,0,10003,7),('seoul-palace',2,1,10004,1),('seoul-palace',2,2,10006,3),
    ('seoul-palace',3,0,10008,10),('seoul-palace',3,1,10007,1),
    ('seoul-night',1,0,10009,5),('seoul-night',1,1,10017,18),('seoul-night',2,0,10018,10),('seoul-night',2,1,10015,5),
    ('seongsu-picnic',1,0,10010,3),('seongsu-picnic',1,1,10011,16),('seongsu-picnic',2,0,10006,7),('seongsu-picnic',2,1,10013,15),
    ('daejeon-science',1,0,20001,12),('daejeon-science',1,1,20002,2),('daejeon-science',1,2,20004,6),
    ('daejeon-science',2,0,20003,12),('daejeon-science',2,1,20005,2),
    ('daejeon-bread',1,0,20006,4),('daejeon-bread',1,1,20012,14),('daejeon-bread',1,2,20014,2),
    ('daejeon-bread',2,0,20007,9),('daejeon-bread',2,1,20008,4),
    ('daejeon-green',1,0,20009,8),('daejeon-green',2,0,20011,13),('daejeon-green',2,1,20010,17),
    ('family-daejeon',1,0,20001,6),('family-daejeon',1,1,20003,12),('family-daejeon',2,0,20013,6),('family-daejeon',2,1,20005,15),
    ('seoul-modern',1,0,10006,7),('seoul-modern',1,1,10004,14),('seoul-modern',2,0,10011,16),('seoul-modern',2,1,10013,7),
    ('seoul-summer',1,0,10009,18),('seoul-summer',1,1,10017,5),('seoul-summer',2,0,10016,18),('seoul-summer',2,1,10015,5),
    ('daejeon-autumn',1,0,20010,17),('daejeon-autumn',1,1,20011,8),('daejeon-autumn',2,0,20005,2),('daejeon-autumn',2,1,20003,17)
)
INSERT INTO itinerary.itinerary_items
  (id,trip_id,itinerary_day_id,sort_order,item_type,place_provider,external_place_id,place_name,address,
   lat,lng,thumbnail_url,source_status,created_by_user_id,updated_by_user_id,created_at,updated_at)
SELECT md5('demo-item:'||i.k||':'||i.d||':'||i.s)::uuid,md5('demo-trip:'||i.k)::uuid,
       md5('demo-day:'||i.k||':'||i.d)::uuid,i.s,'PLACE','KTO',a.content_id::text,a.title,a.addr1,
       a.latitude,a.longitude,a.first_image1,'AVAILABLE',md5('demo-user:'||i.creator)::uuid,
       md5('demo-user:'||i.creator)::uuid,now()-interval '65 days',now()-interval '4 days'
FROM item_seed i JOIN tourism_source.attractions a ON a.content_id=i.content_id
ON CONFLICT (id) DO UPDATE SET place_name=EXCLUDED.place_name,address=EXCLUDED.address,
  lat=EXCLUDED.lat,lng=EXCLUDED.lng,thumbnail_url=EXCLUDED.thumbnail_url,sort_order=EXCLUDED.sort_order;

-- Lightweight route segments connect consecutive items within each day.
INSERT INTO itinerary.trip_routes
  (id,trip_id,origin_itinerary_item_id,destination_itinerary_item_id,mode,provider,provider_profile,
   geometry_format,geometry,distance_meters,duration_seconds,confidence,created_by_user_id,created_at,updated_at)
SELECT md5('demo-route:'||o.id||':'||n.id)::uuid,o.trip_id,o.id,n.id,
       CASE WHEN abs(o.lat-n.lat)+abs(o.lng-n.lng) < .04 THEN 'WALKING' ELSE 'DRIVING' END,
       'MAPBOX',CASE WHEN abs(o.lat-n.lat)+abs(o.lng-n.lng) < .04 THEN 'walking' ELSE 'driving' END,
       'GEOJSON',jsonb_build_object('type','LineString','coordinates',jsonb_build_array(
          jsonb_build_array(o.lng,o.lat),jsonb_build_array(n.lng,n.lat))),
       round((sqrt(power((o.lat-n.lat)::numeric,2)+power((o.lng-n.lng)::numeric,2))*88000)::numeric,2),
       round((sqrt(power((o.lat-n.lat)::numeric,2)+power((o.lng-n.lng)::numeric,2))*88000/1.2)::numeric,2),
       .91,o.created_by_user_id,now()-interval '60 days',now()-interval '3 days'
FROM itinerary.itinerary_items o
JOIN itinerary.itinerary_items n ON n.itinerary_day_id=o.itinerary_day_id AND n.sort_order=o.sort_order+1
WHERE o.id::text IS NOT NULL AND o.created_at >= now()-interval '200 days'
ON CONFLICT (id) DO NOTHING;

-- Notes, checklists and chat make group trips feel actively planned.
INSERT INTO planning.trip_notes (id,trip_id,scope_type,content,created_by_user_id,updated_by_user_id)
SELECT md5('demo-note:'||t.id)::uuid,t.id,'TRIP',
       CASE WHEN t.display_destination LIKE '서울%' THEN '대중교통 위주. 해 질 무렵 야외 장소를 배치하고 비 오면 박물관으로 변경하기.'
            ELSE '대전역을 기준으로 이동. 성심당은 오전 방문, 숲 코스는 운동화 준비하기.' END,
       t.owner_user_id,t.owner_user_id
FROM trip.trips t WHERE t.id IN (SELECT md5('demo-trip:'||k)::uuid FROM (VALUES
 ('seoul-palace'),('seoul-night'),('seongsu-picnic'),('daejeon-science'),('daejeon-bread'),('daejeon-green'),
 ('family-daejeon'),('seoul-modern'),('seoul-summer'),('daejeon-autumn')) x(k))
ON CONFLICT DO NOTHING;

INSERT INTO planning.checklists (id,trip_id,scope_type,title,created_by_user_id,updated_by_user_id)
SELECT md5('demo-checklist:'||t.id)::uuid,t.id,'TRIP','여행 전 함께 확인해요',t.owner_user_id,t.owner_user_id
FROM trip.trips t WHERE t.id IN (SELECT md5('demo-trip:'||k)::uuid FROM (VALUES
 ('seoul-palace'),('seoul-night'),('seongsu-picnic'),('daejeon-science'),('daejeon-bread'),('daejeon-green'),
 ('family-daejeon'),('seoul-modern'),('seoul-summer'),('daejeon-autumn')) x(k))
ON CONFLICT DO NOTHING;

WITH contents(s,content) AS (VALUES (0,'숙소와 교통 예약 확인'),(1,'날씨 확인하고 우산 챙기기'),(2,'공용 경비 정산 준비'),(3,'보조 배터리와 편한 신발'))
INSERT INTO planning.checklist_items (id,checklist_id,sort_order,content,created_by_user_id,updated_by_user_id)
SELECT md5('demo-check-item:'||c.id||':'||x.s)::uuid,c.id,x.s,x.content,c.created_by_user_id,c.created_by_user_id
FROM planning.checklists c CROSS JOIN contents x
WHERE c.id::text IN (SELECT md5('demo-checklist:'||md5('demo-trip:'||k)::uuid)::text FROM (VALUES
 ('seoul-palace'),('seoul-night'),('seongsu-picnic'),('daejeon-science'),('daejeon-bread'),('daejeon-green'),
 ('family-daejeon'),('seoul-modern'),('seoul-summer'),('daejeon-autumn')) z(k))
ON CONFLICT (id) DO NOTHING;

WITH msg(k,u,content,minutes_ago) AS (
 VALUES
 ('seoul-palace',1,'토요일 10시에 경복궁역 5번 출구에서 만나요!',22000),('seoul-palace',3,'북촌은 주민분들 계시니까 조용히 걷자 🙌',21970),
 ('seoul-palace',10,'노을 시간 확인했어. 낙산 대신 덕수궁 돌담길 넣어뒀어.',21920),
 ('daejeon-bread',4,'성심당 오픈 시간 맞춰서 먼저 가는 걸로!',15000),('daejeon-bread',9,'점심은 중앙시장 쪽 칼국수 어때?',14950),
 ('daejeon-bread',2,'좋아. 소제동은 해 질 때 사진 예쁘더라.',14920),
 ('seongsu-picnic',15,'돗자리는 제가 가져갈게요.',8500),('seongsu-picnic',16,'서울숲 다음 전시 예약 완료!',8460),
 ('daejeon-green',17,'계족산 코스 초보도 괜찮지?',4100),('daejeon-green',8,'천천히 걸으면 괜찮아. 물 챙겨가자!',4050),
 ('seoul-summer',18,'반포 분수 시간 확인해서 일정에 적어둘게.',380),('seoul-summer',5,'끝나고 노들섬까지 가면 딱이겠다 🌙',340)
)
INSERT INTO chat.trip_chat_messages (id,trip_id,sender_user_id,content,created_at)
SELECT md5('demo-chat:'||k||':'||u||':'||minutes_ago)::uuid,md5('demo-trip:'||k)::uuid,
       md5('demo-user:'||u)::uuid,content,now()-make_interval(mins=>minutes_ago)
FROM msg ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Trip records and media from completed journeys
-- ---------------------------------------------------------------------------
WITH completed(k) AS (VALUES ('seoul-palace'),('seoul-night'),('seongsu-picnic'),('daejeon-science'),
 ('daejeon-bread'),('daejeon-green'),('seoul-modern')),
items AS (
 SELECT i.*,row_number() OVER (PARTITION BY i.trip_id ORDER BY d.day_number,i.sort_order) rn
 FROM itinerary.itinerary_items i JOIN itinerary.itinerary_days d ON d.id=i.itinerary_day_id
 JOIN completed c ON i.trip_id=md5('demo-trip:'||c.k)::uuid
)
INSERT INTO record.trip_record_entries
 (id,trip_id,itinerary_day_id,itinerary_item_id,uploaded_by_user_id,title,caption,location_name,lat,lng,taken_at,visibility,status,created_at,updated_at)
SELECT md5('demo-record:'||i.id)::uuid,i.trip_id,i.itinerary_day_id,i.id,t.owner_user_id,
       CASE i.rn%4 WHEN 0 THEN '걷다 만난 오늘의 풍경' WHEN 1 THEN i.place_name||'에서 시작한 하루'
                    WHEN 2 THEN '계획보다 더 좋았던 순간' ELSE '우리 여행의 한 장면' END,
       CASE i.rn%3 WHEN 0 THEN '사진보다 현장에서 본 빛과 분위기가 훨씬 좋았다. 다음에도 천천히 다시 걷고 싶은 곳.'
                   WHEN 1 THEN '서두르지 않고 함께 걸어서 더 오래 기억에 남는다.'
                   ELSE '일정에 넣길 정말 잘했다. 근처 골목까지 둘러보면 반나절이 금방 간다.' END,
       i.place_name,i.lat,i.lng,d.date::timestamptz + make_interval(hours=>10+i.sort_order*2),
       'TRIP_MEMBERS','ACTIVE',d.date::timestamptz + make_interval(hours=>10+i.sort_order*2),now()-interval '10 days'
FROM items i JOIN trip.trips t ON t.id=i.trip_id JOIN itinerary.itinerary_days d ON d.id=i.itinerary_day_id
WHERE i.rn<=5
ON CONFLICT (id) DO NOTHING;

INSERT INTO media.media_files
 (id,owner_user_id,bucket,object_key,public_url,mime_type,byte_size,width,height,linked_resource_type,linked_resource_id,status,created_at)
SELECT md5('demo-record-media:'||r.id)::uuid,r.uploaded_by_user_id,'soomgil-local',
       'demo/records/'||r.id||'/cover.jpg',
       'https://picsum.photos/seed/record-'||replace(r.id::text,'-','')||'/1200/900',
       'image/jpeg',420000+(extract(day from r.taken_at)::bigint*731),1200,900,'TRIP_RECORD',r.id,'ACTIVE',r.created_at
FROM record.trip_record_entries r
WHERE r.id IN (SELECT md5('demo-record:'||i.id)::uuid FROM itinerary.itinerary_items i)
ON CONFLICT (id) DO NOTHING;

INSERT INTO record.trip_record_media (record_entry_id,media_file_id,sort_order,caption,created_at)
SELECT r.id,md5('demo-record-media:'||r.id)::uuid,0,'여행에서 직접 남긴 사진',r.created_at
FROM record.trip_record_entries r
WHERE r.id IN (SELECT md5('demo-record:'||i.id)::uuid FROM itinerary.itinerary_items i)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Community feed: published trip snapshots, media, hashtags and engagement
-- ---------------------------------------------------------------------------
WITH post_seed(k,trip_k,publisher,title,summary,published_days) AS (
 VALUES
 ('palace-post','seoul-palace',1,'봄날, 서울 궁궐에서 골목까지 천천히','경복궁 문 여는 시간에 맞춰 시작해 북촌과 익선동, 청계천까지 걸었어요. 이동이 짧아 서울 첫 여행에도 추천하는 2박 3일 코스입니다.',92),
 ('night-post','seoul-night',5,'지하철로 모은 서울 야경 네 장면','노들섬 노을에서 반포, 낙산 성곽, 서울스카이까지. 야경 좋아하는 친구들과 다녀온 동선과 촬영 시간을 남깁니다.',61),
 ('seongsu-post','seongsu-picnic',3,'서울숲 피크닉과 성수의 느린 주말','욕심내지 않고 공원과 성수 골목만 오래 걸었습니다. 돗자리 펴기 좋은 자리와 전시 동선까지 담았어요.',48),
 ('science-post','daejeon-science',12,'어른도 신나는 대전 과학 소풍','국립중앙과학관, 엑스포공원, 시립미술관을 한 동선으로 묶었어요. 비 오는 날에도 좋은 대전 1박 2일 코스!',84),
 ('bread-post','daejeon-bread',4,'성심당만 보고 갔다가 골목에 반한 대전','성심당 오픈런 뒤 근현대사전시관과 테미오래, 소제동까지. 빵 봉투 들고 걷기 좋은 원도심 코스예요.',55),
 ('green-post','daejeon-green',8,'대전에 이런 숲이? 초록 가득 1박 2일','장태산 메타세쿼이아 숲부터 대청호, 계족산 황톳길까지 걷고 왔어요. 차가 있으면 가장 편하지만 대중교통 팁도 적었습니다.',32),
 ('modern-post','seoul-modern',7,'공장을 공원으로, 서울 재생건축 여행','DDP에서 시작해 성수의 붉은 벽돌 공장과 문화비축기지까지. 건축과 공간 재생을 좋아한다면 저장해둘 코스.',110),
 ('family-post','family-daejeon',6,'아이와 갈 대전 여름 여행 미리 짜봤어요','과학관과 수목원, 오월드를 무리 없이 나눈 가족 여행 계획입니다. 실내와 야외 비율을 맞췄어요.',3),
 ('autumn-post','daejeon-autumn',17,'올가을 계족산 황톳길 같이 걸어요','황톳길을 걷고 대청호를 본 뒤 유성온천으로 마무리하는 계획. 다녀온 분들의 팁도 기다릴게요.',1)
)
INSERT INTO community.posts
 (id,source_trip_id,source_trip_version,published_by_user_id,visibility,title,summary,snapshot_version,
  moderation_status,published_at,created_at,updated_at,snapshot)
SELECT md5('demo-post:'||k)::uuid,md5('demo-trip:'||trip_k)::uuid,t.itinerary_version,
       md5('demo-user:'||publisher)::uuid,'PUBLIC',p.title,p.summary,1,'VISIBLE',
       now()-make_interval(days=>published_days),now()-make_interval(days=>published_days),
       now()-make_interval(days=>greatest(published_days-1,0)),'{"days":[],"routes":[],"authorDisplay":null}'::jsonb
FROM post_seed p JOIN trip.trips t ON t.id=md5('demo-trip:'||p.trip_k)::uuid
ON CONFLICT (id) DO UPDATE SET title=EXCLUDED.title,summary=EXCLUDED.summary,
  source_trip_version=EXCLUDED.source_trip_version,updated_at=EXCLUDED.updated_at;

-- Build the exact application snapshot shape from the source itinerary.
UPDATE community.posts p
SET snapshot=jsonb_build_object(
 'days',COALESCE((
   SELECT jsonb_agg(jsonb_build_object(
     'id',d.id,'tripId',d.trip_id,'groupType',d.group_type,'dayNumber',d.day_number,
     'date',d.date,'title',d.title,'sortOrder',d.sort_order,
     'items',COALESCE((SELECT jsonb_agg(jsonb_build_object(
       'id',i.id,'itineraryDayId',i.itinerary_day_id,'sortOrder',i.sort_order,'itemType',i.item_type,
       'placeRef',jsonb_build_object('provider',i.place_provider,'externalPlaceId',i.external_place_id),
       'placeName',i.place_name,'address',i.address,'lat',i.lat,'lng',i.lng,
       'thumbnailUrl',i.thumbnail_url,'sourceStatus',i.source_status) ORDER BY i.sort_order)
       FROM itinerary.itinerary_items i WHERE i.itinerary_day_id=d.id AND i.deleted_at IS NULL),'[]'::jsonb)
   ) ORDER BY d.sort_order) FROM itinerary.itinerary_days d WHERE d.trip_id=p.source_trip_id),'[]'::jsonb),
 'routes',COALESCE((SELECT jsonb_agg(jsonb_build_object(
   'id',r.id,'originItineraryItemId',r.origin_itinerary_item_id,
   'destinationItineraryItemId',r.destination_itinerary_item_id,'mode',r.mode,'provider',r.provider,
   'providerProfile',r.provider_profile,'geometryFormat',r.geometry_format,'geometry',r.geometry,
   'distanceMeters',r.distance_meters,'durationSeconds',r.duration_seconds,'confidence',r.confidence)
   ORDER BY r.created_at) FROM itinerary.trip_routes r WHERE r.trip_id=p.source_trip_id AND r.deleted_at IS NULL),'[]'::jsonb),
 'authorDisplay',(SELECT jsonb_build_object('id',u.user_id,'displayName',u.display_name,
   'profileImageUrl',u.profile_image_url) FROM auth.user_profiles u WHERE u.user_id=p.published_by_user_id)
)
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k));

-- Keep the normalized snapshot tables populated as well as the canonical JSONB snapshot.
INSERT INTO community.post_snapshot_days
  (id,post_id,group_type,day_number,date,title,sort_order)
SELECT md5('demo-post-day:'||p.id||':'||d.id)::uuid,p.id,d.group_type,d.day_number,d.date,d.title,d.sort_order
FROM community.posts p JOIN itinerary.itinerary_days d ON d.trip_id=p.source_trip_id
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
ON CONFLICT (id) DO NOTHING;

INSERT INTO community.post_snapshot_items
  (id,post_id,snapshot_day_id,sort_order,place_provider,external_place_id,source_status,
   place_name,address,lat,lng,thumbnail_url)
SELECT md5('demo-post-item:'||p.id||':'||i.id)::uuid,p.id,
       md5('demo-post-day:'||p.id||':'||d.id)::uuid,i.sort_order,i.place_provider,i.external_place_id,
       i.source_status,i.place_name,i.address,i.lat,i.lng,i.thumbnail_url
FROM community.posts p
JOIN itinerary.itinerary_days d ON d.trip_id=p.source_trip_id
JOIN itinerary.itinerary_items i ON i.itinerary_day_id=d.id AND i.deleted_at IS NULL
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
ON CONFLICT (id) DO NOTHING;

INSERT INTO community.post_snapshot_routes
  (id,post_id,origin_snapshot_item_id,destination_snapshot_item_id,mode,geometry_format,
   geometry,distance_meters,duration_seconds)
SELECT md5('demo-post-route:'||p.id||':'||r.id)::uuid,p.id,
       md5('demo-post-item:'||p.id||':'||r.origin_itinerary_item_id)::uuid,
       md5('demo-post-item:'||p.id||':'||r.destination_itinerary_item_id)::uuid,
       r.mode,r.geometry_format,r.geometry,r.distance_meters,r.duration_seconds
FROM community.posts p
JOIN itinerary.trip_routes r ON r.trip_id=p.source_trip_id AND r.deleted_at IS NULL
JOIN community.post_snapshot_items origin_item
  ON origin_item.id = md5('demo-post-item:'||p.id||':'||r.origin_itinerary_item_id)::uuid
JOIN community.post_snapshot_items destination_item
  ON destination_item.id = md5('demo-post-item:'||p.id||':'||r.destination_itinerary_item_id)::uuid
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
ON CONFLICT (id) DO NOTHING;

WITH tags(name,normalized) AS (VALUES
 ('서울여행','서울여행'),('대전여행','대전여행'),('주말여행','주말여행'),('뚜벅이여행','뚜벅이여행'),
 ('야경명소','야경명소'),('빵지순례','빵지순례'),('아이와가볼만한곳','아이와가볼만한곳'),
 ('숲여행','숲여행'),('골목여행','골목여행'),('데이트코스','데이트코스'),('건축여행','건축여행'),('리트립추천','리트립추천'))
INSERT INTO community.hashtags (id,name,normalized_name,usage_count)
SELECT md5('demo-hashtag:'||normalized)::uuid,name,normalized,0 FROM tags
ON CONFLICT (normalized_name) DO UPDATE SET name=EXCLUDED.name;

WITH mapping(post_k,tag) AS (VALUES
 ('palace-post','서울여행'),('palace-post','뚜벅이여행'),('palace-post','골목여행'),('palace-post','리트립추천'),
 ('night-post','서울여행'),('night-post','야경명소'),('night-post','데이트코스'),
 ('seongsu-post','서울여행'),('seongsu-post','주말여행'),('seongsu-post','데이트코스'),
 ('science-post','대전여행'),('science-post','아이와가볼만한곳'),('science-post','뚜벅이여행'),
 ('bread-post','대전여행'),('bread-post','빵지순례'),('bread-post','골목여행'),('bread-post','리트립추천'),
 ('green-post','대전여행'),('green-post','숲여행'),('green-post','주말여행'),
 ('modern-post','서울여행'),('modern-post','건축여행'),('modern-post','골목여행'),
 ('family-post','대전여행'),('family-post','아이와가볼만한곳'),
 ('autumn-post','대전여행'),('autumn-post','숲여행'),('autumn-post','리트립추천'))
INSERT INTO community.post_hashtags(post_id,hashtag_id)
SELECT md5('demo-post:'||post_k)::uuid,md5('demo-hashtag:'||tag)::uuid FROM mapping
ON CONFLICT DO NOTHING;

UPDATE community.hashtags h SET usage_count=(SELECT count(*) FROM community.post_hashtags ph WHERE ph.hashtag_id=h.id)
WHERE h.id::text IS NOT NULL;

-- A cover asset per post, plus a second gallery image for richer cards/details.
INSERT INTO media.media_files
 (id,owner_user_id,bucket,object_key,public_url,mime_type,byte_size,width,height,linked_resource_type,linked_resource_id,status,created_at)
SELECT md5('demo-post-media:'||p.id||':'||s)::uuid,p.published_by_user_id,'soomgil-local',
       'demo/community/'||p.id||'/'||s||'.jpg',
       'https://picsum.photos/seed/community-'||replace(p.id::text,'-','')||'-'||s||'/1200/800',
       'image/jpeg',510000+s*29000,1200,800,'COMMUNITY_POST',p.id,'ACTIVE',p.published_at
FROM community.posts p CROSS JOIN generate_series(0,1) s
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
ON CONFLICT (id) DO NOTHING;

UPDATE community.posts p SET cover_media_file_id=md5('demo-post-media:'||p.id||':0')::uuid
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k));

INSERT INTO community.post_media(id,post_id,media_file_id,sort_order,caption,created_at)
SELECT md5('demo-post-media-link:'||p.id||':'||s)::uuid,p.id,md5('demo-post-media:'||p.id||':'||s)::uuid,s,
       CASE s WHEN 0 THEN '여행의 대표 장면' ELSE '코스에서 오래 기억에 남은 순간' END,p.published_at
FROM community.posts p CROSS JOIN generate_series(0,1) s
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
ON CONFLICT DO NOTHING;

-- Dense but varied social engagement.
INSERT INTO community.post_likes(post_id,user_id,created_at)
SELECT p.id,md5('demo-user:'||u)::uuid,p.published_at+make_interval(hours=>u*9)
FROM community.posts p CROSS JOIN generate_series(1,20) u
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
  AND (get_byte(decode(md5(p.id::text||u),'hex'),0)+u)%4<>0
  AND md5('demo-user:'||u)::uuid<>p.published_by_user_id
ON CONFLICT DO NOTHING;

WITH comment(post_k,u,content,days_after) AS (VALUES
 ('palace-post',11,'북촌에서 익선동으로 넘어갈 때 도보로 괜찮았나요?',1),
 ('palace-post',1,'네! 중간에 카페 한 번 쉬면 여유롭게 걸을 만했어요.',1),
 ('palace-post',14,'돌담길까지 넣은 동선 너무 좋네요. 바로 저장했어요.',2),
 ('night-post',10,'낙산공원 사진은 몇 시쯤 찍으셨어요? 빛이 정말 예뻐요.',1),
 ('night-post',5,'해 지고 30분쯤 뒤예요. 삼각대 없어도 난간 활용하면 괜찮아요!',1),
 ('seongsu-post',15,'서울숲 피크닉 자리는 주말 몇 시 전에 가야 할까요?',2),
 ('seongsu-post',3,'11시 전에는 꽤 여유 있었어요. 그늘 쪽은 조금 빨리 차더라고요.',2),
 ('science-post',6,'아이랑 가면 과학관에서 몇 시간 정도 잡는 게 좋을까요?',1),
 ('science-post',12,'최소 3시간 추천해요. 체험 예약 가능한 전시는 먼저 확인하세요!',1),
 ('science-post',19,'비 오는 날 대전 코스로 딱이네요.',3),
 ('bread-post',9,'이 동선 그대로 리트립했어요. 칼국수집도 끼워 넣으려고요 😋',1),
 ('bread-post',14,'테미오래가 생각보다 사진 찍기 정말 좋았어요.',2),
 ('bread-post',2,'대전역 보관함에 빵 맡기면 걷기 훨씬 편합니다!',2),
 ('green-post',17,'장태산 스카이웨이 고소공포증 있어도 괜찮나요?',1),
 ('green-post',8,'아래 숲길만 걸어도 충분히 좋아요. 무리하지 않아도 됩니다.',1),
 ('modern-post',16,'전시 좋아하는 친구랑 이 코스 꼭 가보고 싶어요.',4),
 ('modern-post',7,'DDP 전시 일정 확인하고 가면 더 알차요!',4),
 ('family-post',12,'과학관 체험은 온라인 예약 열리면 금방 마감돼요.',1),
 ('autumn-post',8,'10월 중순이면 숲 색도 예쁘고 걷기 딱 좋겠네요.',0),
 ('autumn-post',2,'걷고 나서 족욕으로 마무리하는 코스 찬성!',0)
)
INSERT INTO community.post_comments
 (id,post_id,author_user_id,content,depth,moderation_status,created_at,updated_at)
SELECT md5('demo-comment:'||post_k||':'||u||':'||days_after)::uuid,md5('demo-post:'||post_k)::uuid,
       md5('demo-user:'||u)::uuid,content,0,'VISIBLE',p.published_at+make_interval(days=>days_after,hours=>u),
       p.published_at+make_interval(days=>days_after,hours=>u)
FROM comment c JOIN community.posts p ON p.id=md5('demo-post:'||c.post_k)::uuid
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Retrips: copied independent trips with provenance and copied itinerary
-- ---------------------------------------------------------------------------
WITH retrip(k,post_k,u,title,created_days) AS (VALUES
 ('retrip-palace','palace-post',19,'혼자 걷는 서울 궁궐과 골목',18),
 ('retrip-bread','bread-post',3,'친구들과 대전 빵지순례',11),
 ('retrip-science','science-post',6,'우리 가족 대전 과학여행',6),
 ('retrip-green','green-post',17,'초보도 걷는 대전 숲 코스',3)
)
INSERT INTO trip.trips
 (id,owner_user_id,title,display_destination,status,itinerary_version,retripped_from_post_id,
  retripped_from_snapshot_version,created_at,updated_at)
SELECT md5('demo-trip:'||r.k)::uuid,md5('demo-user:'||r.u)::uuid,r.title,
       CASE WHEN r.post_k='palace-post' THEN '서울 종로·중구' ELSE '대전광역시' END,
       'ACTIVE',1,md5('demo-post:'||r.post_k)::uuid,1,now()-make_interval(days=>r.created_days),now()-interval '3 hours'
FROM retrip r ON CONFLICT (id) DO NOTHING;

WITH retrip(k,post_k,u) AS (VALUES
 ('retrip-palace','palace-post',19),('retrip-bread','bread-post',3),
 ('retrip-science','science-post',6),('retrip-green','green-post',17))
INSERT INTO trip.trip_members(id,trip_id,user_id,role,status,joined_at)
SELECT md5('demo-member:'||k||':'||u)::uuid,md5('demo-trip:'||k)::uuid,md5('demo-user:'||u)::uuid,
       'MEMBER','ACTIVE',now()-interval '5 days' FROM retrip
ON CONFLICT (trip_id,user_id) DO NOTHING;

WITH retrip(k,post_k) AS (VALUES
 ('retrip-palace','palace-post'),('retrip-bread','bread-post'),
 ('retrip-science','science-post'),('retrip-green','green-post')),
src AS (SELECT r.k,r.post_k,p.source_trip_id,new_t.id new_trip_id
        FROM retrip r JOIN community.posts p ON p.id=md5('demo-post:'||r.post_k)::uuid
        JOIN trip.trips new_t ON new_t.id=md5('demo-trip:'||r.k)::uuid)
INSERT INTO itinerary.itinerary_days(id,trip_id,group_type,day_number,date,title,sort_order,created_at,updated_at)
SELECT md5('demo-retrip-day:'||s.k||':'||d.id)::uuid,s.new_trip_id,d.group_type,d.day_number,
       current_date+10+d.day_number,d.title,d.sort_order,now()-interval '4 days',now()
FROM src s JOIN itinerary.itinerary_days d ON d.trip_id=s.source_trip_id
ON CONFLICT (id) DO NOTHING;

WITH retrip(k,post_k,u) AS (VALUES
 ('retrip-palace','palace-post',19),('retrip-bread','bread-post',3),
 ('retrip-science','science-post',6),('retrip-green','green-post',17)),
src AS (SELECT r.*,p.source_trip_id,md5('demo-trip:'||r.k)::uuid new_trip_id
        FROM retrip r JOIN community.posts p ON p.id=md5('demo-post:'||r.post_k)::uuid)
INSERT INTO itinerary.itinerary_items
 (id,trip_id,itinerary_day_id,sort_order,item_type,place_provider,external_place_id,place_name,address,lat,lng,
  thumbnail_url,source_status,created_by_user_id,updated_by_user_id,created_at,updated_at)
SELECT md5('demo-retrip-item:'||s.k||':'||i.id)::uuid,s.new_trip_id,
       md5('demo-retrip-day:'||s.k||':'||i.itinerary_day_id)::uuid,i.sort_order,i.item_type,
       i.place_provider,i.external_place_id,i.place_name,i.address,i.lat,i.lng,i.thumbnail_url,i.source_status,
       md5('demo-user:'||s.u)::uuid,md5('demo-user:'||s.u)::uuid,now()-interval '4 days',now()
FROM src s JOIN itinerary.itinerary_items i ON i.trip_id=s.source_trip_id AND i.deleted_at IS NULL
ON CONFLICT (id) DO NOTHING;

WITH retrip(k,post_k,u) AS (VALUES
 ('retrip-palace','palace-post',19),('retrip-bread','bread-post',3),
 ('retrip-science','science-post',6),('retrip-green','green-post',17))
INSERT INTO community.post_retrips(id,post_id,user_id,new_trip_id,snapshot_version,created_at)
SELECT md5('demo-retrip:'||k)::uuid,md5('demo-post:'||post_k)::uuid,md5('demo-user:'||u)::uuid,
       md5('demo-trip:'||k)::uuid,1,now()-interval '2 days' FROM retrip
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Social graph, notifications and operational traces
-- ---------------------------------------------------------------------------
INSERT INTO social.user_follows(follower_user_id,following_user_id,status,created_at,updated_at)
SELECT md5('demo-user:'||a)::uuid,md5('demo-user:'||b)::uuid,'ACTIVE',
       now()-make_interval(days=>((a*7+b*3)%120)),now()-make_interval(hours=>((a+b)%72))
FROM generate_series(1,20) a CROSS JOIN generate_series(1,20) b
WHERE a<>b AND ((a*7+b*11)%5=0 OR b IN (1,2,3,4,8,12))
ON CONFLICT (follower_user_id,following_user_id) DO NOTHING;

INSERT INTO notification.notifications
 (id,recipient_user_id,actor_user_id,trip_id,type,title,body,payload,read_at,created_at)
SELECT md5('demo-notification:like:'||l.post_id||':'||l.user_id)::uuid,p.published_by_user_id,l.user_id,
       p.source_trip_id,'COMMUNITY_POST_LIKED','새로운 좋아요','회원님이 여행 게시물을 좋아합니다.',
       jsonb_build_object('postId',p.id),CASE WHEN row_number() OVER (ORDER BY l.created_at)%3=0 THEN l.created_at+interval '2 hours' END,l.created_at
FROM community.post_likes l JOIN community.posts p ON p.id=l.post_id
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
  AND (get_byte(decode(md5(l.user_id::text),'hex'),0)%7=0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO notification.notifications
 (id,recipient_user_id,actor_user_id,trip_id,type,title,body,payload,created_at)
SELECT md5('demo-notification:retrip:'||r.id)::uuid,p.published_by_user_id,r.user_id,p.source_trip_id,
       'COMMUNITY_POST_RETRIPPED','내 여행이 리트립됐어요','누군가 회원님의 여행 코스로 새 여행을 시작했습니다.',
       jsonb_build_object('postId',p.id,'newTripId',r.new_trip_id),r.created_at
FROM community.post_retrips r JOIN community.posts p ON p.id=r.post_id
ON CONFLICT (id) DO NOTHING;

INSERT INTO ops.audit_logs(actor_user_id,action,target_type,target_id,metadata,created_at)
SELECT p.published_by_user_id,'COMMUNITY_POST_PUBLISHED','COMMUNITY_POST',p.id,
       jsonb_build_object('sourceTripId',p.source_trip_id,'seed','seoul-daejeon-demo'),p.published_at
FROM community.posts p
WHERE p.id IN (SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),
 ('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post')) x(k))
AND NOT EXISTS (SELECT 1 FROM ops.audit_logs o WHERE o.action='COMMUNITY_POST_PUBLISHED' AND o.target_id=p.id);

-- ===========================================================================
-- Dense expansion pack
-- Adds broader real-place search coverage and production-like activity volume.
-- ===========================================================================

-- More well-known, real-world Seoul and Daejeon destinations.
WITH place(content_id,title,content_type_id,area_code,gugun,lat,lng,tel,addr,homepage,overview,seed) AS (
  VALUES
    (10019,'창덕궁',12,1,23,37.579430,126.991040,'02-3668-2300','서울특별시 종로구 율곡로 99','https://royal.khs.go.kr/','후원과 자연 지형을 살린 궁궐 배치로 유명한 유네스코 세계유산.','changdeokgung'),
    (10020,'창경궁',12,1,23,37.578740,126.995060,'02-762-4868','서울특별시 종로구 창경궁로 185','https://royal.khs.go.kr/','연못과 온실, 고궁 산책을 함께 즐길 수 있는 조선시대 궁궐.','changgyeonggung'),
    (10021,'종묘',12,1,23,37.574630,126.994170,'02-765-0195','서울특별시 종로구 종로 157','https://royal.khs.go.kr/','조선 왕과 왕비의 신위를 모신 유네스코 세계유산.','jongmyo'),
    (10022,'광화문광장',12,1,23,37.572390,126.976930,NULL,'서울특별시 종로구 세종대로 175',NULL,'경복궁과 세종문화회관 사이에 펼쳐진 서울 도심 대표 광장.','gwanghwamun-square'),
    (10023,'서울역사박물관',14,1,23,37.570420,126.970660,'02-724-0274','서울특별시 종로구 새문안로 55','https://museum.seoul.go.kr/','조선시대부터 현대까지 서울의 변화와 생활사를 보여주는 박물관.','seoul-history-museum'),
    (10024,'광장시장',38,1,23,37.570140,126.999530,'02-2267-0291','서울특별시 종로구 창경궁로 88',NULL,'빈대떡과 육회 등 다양한 먹거리로 유명한 서울의 전통시장.','gwangjang-market'),
    (10025,'남산골한옥마을',12,1,24,37.559340,126.994580,'02-6358-5533','서울특별시 중구 퇴계로34길 28','https://www.hanokmaeul.or.kr/','전통 한옥과 정원, 생활문화 체험을 만나는 도심 속 마을.','namsangol'),
    (10026,'서울시립미술관 서소문본관',14,1,24,37.564080,126.973750,'02-2124-8800','서울특별시 중구 덕수궁길 61','https://sema.seoul.go.kr/','덕수궁 돌담길 옆에서 현대미술 전시를 만나는 시립미술관.','sema'),
    (10027,'전쟁기념관',14,1,21,37.536560,126.977020,'02-709-3114','서울특별시 용산구 이태원로 29','https://www.warmemo.or.kr/','한국의 전쟁사와 평화의 의미를 살펴보는 대형 역사문화시설.','war-memorial'),
    (10028,'리움미술관',14,1,21,37.538490,126.999020,'02-2014-6900','서울특별시 용산구 이태원로55길 60-16','https://www.leeumhoam.org/','고미술과 현대미술, 독특한 건축을 함께 경험하는 미술관.','leeum'),
    (10029,'뚝섬한강공원',12,1,16,37.529330,127.073020,NULL,'서울특별시 광진구 강변북로 139',NULL,'자전거와 수상활동, 피크닉을 즐기기 좋은 한강공원.','ttukseom-hangang'),
    (10030,'서울어린이대공원',12,1,16,37.549450,127.081070,'02-450-9311','서울특별시 광진구 능동로 216','https://www.sisul.or.kr/open_content/childrenpark/','동물원과 놀이시설, 넓은 산책로를 갖춘 가족 공원.','children-grand-park'),
    (10031,'코엑스 별마당도서관',14,1,1,37.511680,127.059150,NULL,'서울특별시 강남구 영동대로 513','https://www.starfield.co.kr/coexmall/','높은 서가와 열린 독서 공간으로 유명한 실내 문화 명소.','starfield-library'),
    (10032,'봉은사',12,1,1,37.515150,127.057220,'02-3218-4800','서울특별시 강남구 봉은사로 531','https://www.bongeunsa.org/','도심 고층 건물 사이에서 고즈넉한 분위기를 느끼는 전통 사찰.','bongeunsa'),
    (10033,'석촌호수',12,1,18,37.508670,127.101100,NULL,'서울특별시 송파구 잠실동 47',NULL,'벚꽃과 야경, 호수 산책으로 사랑받는 잠실의 대표 휴식공간.','seokchon-lake'),
    (10034,'송리단길',12,1,18,37.510340,127.109850,NULL,'서울특별시 송파구 백제고분로45길 일대',NULL,'작은 식당과 카페가 모여 있는 석촌호수 인근 골목.','songridan'),
    (10035,'서울식물원',12,1,1,37.569370,126.835080,'02-2104-9716','서울특별시 강서구 마곡동로 161','https://botanicpark.seoul.go.kr/','대형 온실과 주제정원, 호수공원을 함께 즐기는 도심 식물원.','seoul-botanic'),
    (10036,'선유도공원',12,1,14,37.543980,126.900240,'02-2631-9368','서울특별시 영등포구 선유로 343',NULL,'옛 정수장을 생태와 수변 공간으로 재생한 한강의 섬 공원.','seonyudo'),
    (10037,'경의선숲길',12,1,14,37.556960,126.924150,NULL,'서울특별시 마포구 연남동 일대',NULL,'폐철길을 따라 카페와 동네 풍경이 이어지는 선형 공원.','gyeongui-forest'),
    (10038,'서대문형무소역사관',14,1,23,37.574270,126.956070,'02-360-8590','서울특별시 서대문구 통일로 251','https://sphh.sscmc.or.kr/','독립운동과 민주화의 역사를 기억하는 근현대사 현장.','seodaemun-prison'),
    (10039,'북한산국립공원',12,1,23,37.659520,126.977990,'02-909-0497','서울특별시 성북구 보국문로 262',NULL,'서울 도심 가까이에서 능선과 계곡을 만나는 국립공원.','bukhansan'),
    (10040,'은평한옥마을',12,1,23,37.640830,126.938080,NULL,'서울특별시 은평구 연서로50길 일대',NULL,'북한산을 배경으로 현대 한옥과 골목을 걷는 마을.','eunpyeong-hanok'),
    (20015,'이응노미술관',14,3,3,36.366860,127.387350,'042-611-9800','대전광역시 서구 둔산대로 157','https://www.leeungnomuseum.or.kr/','고암 이응노의 작품과 건축, 정원을 함께 감상하는 미술관.','leeungno'),
    (20016,'대전예술의전당',14,3,3,36.366950,127.385310,'042-270-8333','대전광역시 서구 둔산대로 135','https://www.daejeon.go.kr/djac/','공연과 음악회를 관람하는 대전의 대표 복합공연장.','daejeon-arts-center'),
    (20017,'엑스포다리',12,3,4,36.374880,127.389750,NULL,'대전광역시 유성구 도룡동',NULL,'갑천과 한빛탑을 잇는 야경 명소로 산책과 자전거에 좋다.','expo-bridge'),
    (20018,'화폐박물관',14,3,4,36.378130,127.370500,'042-870-1200','대전광역시 유성구 과학로 80-67','https://museum.komsco.com/','화폐의 역사와 제조 과정을 체험하는 한국조폐공사 박물관.','currency-museum'),
    (20019,'지질박물관',14,3,4,36.381090,127.360500,'042-868-3798','대전광역시 유성구 과학로 124','https://museum.kigam.re.kr/','광물과 화석, 지구의 역사를 만나는 전문 과학박물관.','geological-museum'),
    (20020,'유림공원',12,3,4,36.360740,127.358460,NULL,'대전광역시 유성구 어은로 27',NULL,'계절 꽃과 국화축제로 알려진 갑천변 도심 공원.','yurim-park'),
    (20021,'으능정이문화의거리',12,3,5,36.328650,127.426820,NULL,'대전광역시 중구 중앙로 164',NULL,'대전 원도심 쇼핑과 먹거리, 스카이로드가 이어지는 보행 거리.','euneungjeongi'),
    (20022,'대전스카이로드',12,3,5,36.328460,127.428360,NULL,'대전광역시 중구 중앙로164번길 17',NULL,'대형 LED 영상 아케이드가 설치된 대전 원도심의 야간 명소.','skyroad'),
    (20023,'뿌리공원',12,3,5,36.285210,127.388200,'042-288-8310','대전광역시 중구 뿌리공원로 79','https://www.djjunggu.go.kr/hyo/','성씨 조형물과 산책로, 족보박물관이 있는 가족 공원.','ppuri-park'),
    (20024,'보문산 전망대',12,3,5,36.312800,127.419900,NULL,'대전광역시 중구 보문산공원로 일대',NULL,'대전 도심을 내려다보며 가볍게 걷기 좋은 보문산 전망 명소.','bomunsan'),
    (20025,'상소동 산림욕장',12,3,2,36.217750,127.467300,'042-273-4174','대전광역시 동구 산내로 714',NULL,'돌탑과 숲길, 물놀이장이 어우러진 이국적인 산림 휴양지.','sangso-forest'),
    (20026,'식장산 해돋이전망대',12,3,2,36.311550,127.481940,NULL,'대전광역시 동구 세천동 산43-5',NULL,'대전 시내와 대청호 방향의 야경과 일출을 보는 전망대.','sikjangsan'),
    (20027,'동춘당공원',12,3,1,36.365240,127.441740,'042-608-6572','대전광역시 대덕구 동춘당로 80',NULL,'조선시대 고택 동춘당과 너른 마당을 만나는 역사공원.','dongchundang'),
    (20028,'대청댐 물문화관',14,3,1,36.477730,127.480890,'042-930-7332','대전광역시 대덕구 대청로 618-136',NULL,'대청호와 물의 소중함을 전시로 배우고 전망을 즐기는 공간.','daecheong-dam')
)
INSERT INTO tourism_source.attractions
 (content_id,title,content_type_id,area_code,si_gun_gu_code,first_image1,first_image2,map_level,
  latitude,longitude,tel,addr1,homepage,overview,source_hash,source_modified_at,imported_at)
SELECT content_id,title,content_type_id,area_code,gugun,
 'https://picsum.photos/seed/soomgil-'||seed||'/1200/800',
 'https://picsum.photos/seed/soomgil-'||seed||'-detail/800/600',6,lat,lng,tel,addr,homepage,overview,
 md5(title||':'||addr),now(),now() FROM place
ON CONFLICT (content_id) DO UPDATE SET title=EXCLUDED.title,addr1=EXCLUDED.addr1,
 latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,overview=EXCLUDED.overview,
 first_image1=EXCLUDED.first_image1,first_image2=EXCLUDED.first_image2;

INSERT INTO tourism_source.attraction_images
 (id,attraction_no,source_provider,source_type,original_url,public_url,display_order,width,height,is_active)
SELECT md5('demo-attraction-image:'||a.content_id)::uuid,a.no,'DEMO','PRIMARY',a.first_image1,a.first_image1,0,1200,800,true
FROM tourism_source.attractions a WHERE a.content_id BETWEEN 10019 AND 10040 OR a.content_id BETWEEN 20015 AND 20028
ON CONFLICT (id) DO UPDATE SET public_url=EXCLUDED.public_url,is_active=true;

INSERT INTO preference.place_tag_enrichments
 (id,provider,external_place_id,source_hash,status,model_provider,model_name,prompt_version,
  tag_dictionary_version,selection_policy_version,candidate_count,selected_count,enriched_at)
SELECT md5('demo-enrichment:'||content_id)::uuid,'KTO',content_id::text,source_hash,'SUCCEEDED','CURATED',
 'soomgil-demo-curator','demo-v2','preference-tags-v1','demo-policy-v1',3,3,now()
FROM tourism_source.attractions WHERE content_id BETWEEN 10019 AND 10040 OR content_id BETWEEN 20015 AND 20028
ON CONFLICT (id) DO UPDATE SET status='SUCCEEDED',selected_count=3,enriched_at=now();

-- Give expanded places broad, searchable preference facets based on facility type and region.
WITH candidates AS (
 SELECT a.content_id,e.id enrichment_id,
   CASE WHEN a.content_type_id=14 THEN ARRAY['indoor','viewing','educational']
        WHEN a.content_type_id=38 THEN ARRAY['lively','local_culture','street_alley']
        ELSE ARRAY['walking','photo_spot',CASE WHEN a.area_code=1 THEN 'urban' ELSE 'healing' END] END tags
 FROM tourism_source.attractions a JOIN preference.place_tag_enrichments e
   ON e.provider='KTO' AND e.external_place_id=a.content_id::text
 WHERE a.content_id BETWEEN 10019 AND 10040 OR a.content_id BETWEEN 20015 AND 20028
), expanded AS (
 SELECT c.*,u.tag_code,u.ord FROM candidates c CROSS JOIN LATERAL unnest(c.tags) WITH ORDINALITY u(tag_code,ord)
)
INSERT INTO preference.place_tag_enrichment_tags
 (enrichment_id,tag_id,confidence,weight,selection_score,rank_order,rationale)
SELECT x.enrichment_id,t.id,0.92-(x.ord-1)*0.05,1.05-(x.ord-1)*0.05,
 round(((0.92-(x.ord-1)*0.05)*(1.05-(x.ord-1)*0.05))::numeric,6),x.ord,'확장 데모 장소 분류'
FROM expanded x JOIN preference.preference_tags t ON t.code=x.tag_code
ON CONFLICT (enrichment_id,tag_id) DO NOTHING;

-- 100 additional realistic-looking community accounts (total: 120).
WITH names AS (
 SELECT ARRAY['김하늘','이서준','박지민','최유진','정도현','강서연','조민재','윤지아','장현준','임수아',
  '한예준','오채원','서도윤','신예은','권민준','황지원','안시우','송나경','전하준','홍다인',
  '문재윤','양소희','고태윤','배은서','백준영','허지안','남승현','심가은','노현우','하보민'] arr
), bios AS (
 SELECT ARRAY['주말마다 가까운 도시를 걷고 기록해요.','좋은 풍경과 맛있는 한 끼를 찾아다닙니다.',
  '계획은 꼼꼼하게, 여행은 느긋하게.','대중교통으로 갈 수 있는 여행지를 좋아해요.',
  '사진보다 기억에 오래 남는 여행을 만들고 싶어요.','공원과 전시, 조용한 골목을 자주 찾습니다.',
  '친구들과 함께 쓸 수 있는 여행 지도를 만드는 중.','서울과 대전 사이를 자주 오가는 생활 여행자입니다.'] arr
)
INSERT INTO auth.users(id,status,status_changed_at,last_login_at,created_at,updated_at)
SELECT md5('demo-user:'||n)::uuid,'ACTIVE',now()-make_interval(days=>220-(n%180)),
 now()-make_interval(hours=>(n*13)%360),now()-make_interval(days=>220-(n%180)),now()-make_interval(hours=>n%96)
FROM generate_series(21,120)n ON CONFLICT(id) DO NOTHING;

WITH names AS (
 SELECT ARRAY['김하늘','이서준','박지민','최유진','정도현','강서연','조민재','윤지아','장현준','임수아',
  '한예준','오채원','서도윤','신예은','권민준','황지원','안시우','송나경','전하준','홍다인',
  '문재윤','양소희','고태윤','배은서','백준영','허지안','남승현','심가은','노현우','하보민'] arr
), bios AS (
 SELECT ARRAY['주말마다 가까운 도시를 걷고 기록해요.','좋은 풍경과 맛있는 한 끼를 찾아다닙니다.',
  '계획은 꼼꼼하게, 여행은 느긋하게.','대중교통으로 갈 수 있는 여행지를 좋아해요.',
  '사진보다 기억에 오래 남는 여행을 만들고 싶어요.','공원과 전시, 조용한 골목을 자주 찾습니다.',
  '친구들과 함께 쓸 수 있는 여행 지도를 만드는 중.','서울과 대전 사이를 자주 오가는 생활 여행자입니다.'] arr
)
INSERT INTO auth.user_profiles(user_id,display_name,profile_image_url,bio,profile_visibility,created_at,updated_at)
SELECT md5('demo-user:'||n)::uuid,names.arr[1+((n-21)%30)]||CASE WHEN n>50 THEN ' '||lpad(n::text,3,'0') ELSE '' END,
 'https://i.pravatar.cc/300?u=soomgil-community-'||n,bios.arr[1+((n-21)%8)],'PUBLIC',
 now()-make_interval(days=>220-(n%180)),now()-make_interval(hours=>n%96)
FROM generate_series(21,120)n CROSS JOIN names CROSS JOIN bios
ON CONFLICT(user_id) DO UPDATE SET display_name=EXCLUDED.display_name,bio=EXCLUDED.bio,
 profile_image_url=EXCLUDED.profile_image_url;

INSERT INTO auth.user_email_addresses(id,user_id,email,normalized_email,is_primary,verified_at,created_at,updated_at)
SELECT md5('demo-email:'||n)::uuid,md5('demo-user:'||n)::uuid,'member'||n||'@soomgil.local',
 'member'||n||'@soomgil.local',true,now()-interval '60 days',now()-interval '61 days',now()
FROM generate_series(21,120)n ON CONFLICT DO NOTHING;

INSERT INTO auth.user_settings(user_id,display_language,timezone,marketing_email_opt_in,trip_invite_email_opt_in,created_at,updated_at)
SELECT md5('demo-user:'||n)::uuid,'ko','Asia/Seoul',n%4=0,true,now()-interval '60 days',now()
FROM generate_series(21,120)n ON CONFLICT(user_id) DO NOTHING;

-- Expanded users create thousands of preference and social signals.
INSERT INTO preference.user_place_reactions
 (id,user_id,provider,external_place_id,reaction,reaction_count,first_reacted_at,last_reacted_at,place_tag_enrichment_id)
SELECT md5('demo-reaction:'||u||':'||a.content_id)::uuid,md5('demo-user:'||u)::uuid,'KTO',a.content_id::text,
 CASE WHEN (u+a.content_id)%13=0 THEN 'NOPE' WHEN (u*5+a.content_id)%9=0 THEN 'SUPER_LIKE' ELSE 'LIKE' END,
 1,now()-make_interval(days=>((u+a.content_id)%120)),now()-make_interval(hours=>((u*17+a.content_id)%500)),
 md5('demo-enrichment:'||a.content_id)::uuid
FROM generate_series(21,120)u CROSS JOIN tourism_source.attractions a
WHERE a.content_id IN (SELECT content_id FROM tourism_source.attractions WHERE
 content_id BETWEEN 10001 AND 10040 OR content_id BETWEEN 20001 AND 20028)
AND (u*11+a.content_id)%5<>0
ON CONFLICT(user_id,provider,external_place_id) DO NOTHING;

INSERT INTO preference.user_saved_places(id,user_id,provider,external_place_id,created_at)
SELECT md5('demo-save:'||r.user_id||':'||r.provider||':'||r.external_place_id)::uuid,
 r.user_id,r.provider,r.external_place_id,r.last_reacted_at
FROM preference.user_place_reactions r
WHERE r.user_id IN (SELECT md5('demo-user:'||u)::uuid FROM generate_series(21,120)u)
AND r.reaction='SUPER_LIKE'
ON CONFLICT(user_id,provider,external_place_id) DO NOTHING;

INSERT INTO social.user_follows(follower_user_id,following_user_id,status,created_at,updated_at)
SELECT md5('demo-user:'||a)::uuid,md5('demo-user:'||b)::uuid,'ACTIVE',
 now()-make_interval(days=>((a*5+b*3)%180)),now()-make_interval(hours=>((a+b)%96))
FROM generate_series(1,120)a CROSS JOIN generate_series(1,120)b
WHERE a<>b AND ((a*13+b*7)%17=0 OR (b<=20 AND (a+b)%6=0))
ON CONFLICT(follower_user_id,following_user_id) DO NOTHING;

-- Fifty additional trips are derived from the carefully authored templates.
WITH templates(idx,k) AS (VALUES (1,'seoul-palace'),(2,'seoul-night'),(3,'seongsu-picnic'),
 (4,'daejeon-science'),(5,'daejeon-bread'),(6,'daejeon-green'),(7,'family-daejeon'),
 (8,'seoul-modern'),(9,'seoul-summer'),(10,'daejeon-autumn')),
bulk AS (
 SELECT g,t.k,21+((g*7)%100) owner,
  CASE WHEN t.k LIKE 'seoul%' OR t.k IN('seongsu-picnic') THEN '서울' ELSE '대전' END city
 FROM generate_series(1,50)g JOIN templates t ON t.idx=1+((g-1)%10)
)
INSERT INTO trip.trips(id,owner_user_id,title,display_destination,status,itinerary_version,created_at,updated_at)
SELECT md5('demo-bulk-trip:'||g)::uuid,md5('demo-user:'||owner)::uuid,
 CASE g%6 WHEN 0 THEN city||'에서 보낸 우리들의 주말' WHEN 1 THEN city||' 뚜벅이 1박 2일'
  WHEN 2 THEN '친구들과 다시 가는 '||city WHEN 3 THEN city||' 사진 산책'
  WHEN 4 THEN '천천히 걷는 '||city||' 여행' ELSE city||' 맛과 풍경 모으기' END||' #'||g,
 city||' 주요 명소','ACTIVE',3+(g%9),now()-make_interval(days=>30+(g*3)%240),now()-make_interval(hours=>g%120)
FROM bulk ON CONFLICT(id) DO NOTHING;

WITH templates(idx,k) AS (VALUES (1,'seoul-palace'),(2,'seoul-night'),(3,'seongsu-picnic'),
 (4,'daejeon-science'),(5,'daejeon-bread'),(6,'daejeon-green'),(7,'family-daejeon'),
 (8,'seoul-modern'),(9,'seoul-summer'),(10,'daejeon-autumn')),
bulk AS (SELECT g,t.k,21+((g*7)%100) owner FROM generate_series(1,50)g JOIN templates t ON t.idx=1+((g-1)%10))
INSERT INTO trip.trip_members(id,trip_id,user_id,role,status,joined_at)
SELECT md5('demo-bulk-member:'||g||':'||u)::uuid,md5('demo-bulk-trip:'||g)::uuid,
 md5('demo-user:'||u)::uuid,'MEMBER','ACTIVE',now()-make_interval(days=>20+g)
FROM bulk CROSS JOIN LATERAL (VALUES(owner),(21+((owner+17)%100)),(21+((owner+43)%100)))m(u)
ON CONFLICT(trip_id,user_id) DO NOTHING;

WITH templates(idx,k) AS (VALUES (1,'seoul-palace'),(2,'seoul-night'),(3,'seongsu-picnic'),
 (4,'daejeon-science'),(5,'daejeon-bread'),(6,'daejeon-green'),(7,'family-daejeon'),
 (8,'seoul-modern'),(9,'seoul-summer'),(10,'daejeon-autumn')),
bulk AS (SELECT g,t.k FROM generate_series(1,50)g JOIN templates t ON t.idx=1+((g-1)%10))
INSERT INTO itinerary.itinerary_days(id,trip_id,group_type,day_number,date,title,sort_order,created_at,updated_at)
SELECT md5('demo-bulk-day:'||b.g||':'||d.id)::uuid,md5('demo-bulk-trip:'||b.g)::uuid,d.group_type,
 d.day_number,current_date-(20+(b.g%180))+d.day_number,d.title,d.sort_order,
 now()-make_interval(days=>30+b.g),now()-make_interval(hours=>b.g)
FROM bulk b JOIN itinerary.itinerary_days d ON d.trip_id=md5('demo-trip:'||b.k)::uuid
ON CONFLICT(id) DO NOTHING;

WITH templates(idx,k) AS (VALUES (1,'seoul-palace'),(2,'seoul-night'),(3,'seongsu-picnic'),
 (4,'daejeon-science'),(5,'daejeon-bread'),(6,'daejeon-green'),(7,'family-daejeon'),
 (8,'seoul-modern'),(9,'seoul-summer'),(10,'daejeon-autumn')),
bulk AS (SELECT g,t.k,21+((g*7)%100)owner FROM generate_series(1,50)g JOIN templates t ON t.idx=1+((g-1)%10))
INSERT INTO itinerary.itinerary_items
 (id,trip_id,itinerary_day_id,sort_order,item_type,place_provider,external_place_id,place_name,address,lat,lng,
 thumbnail_url,source_status,created_by_user_id,updated_by_user_id,created_at,updated_at)
SELECT md5('demo-bulk-item:'||b.g||':'||i.id)::uuid,md5('demo-bulk-trip:'||b.g)::uuid,
 md5('demo-bulk-day:'||b.g||':'||i.itinerary_day_id)::uuid,i.sort_order,i.item_type,i.place_provider,
 i.external_place_id,i.place_name,i.address,i.lat,i.lng,i.thumbnail_url,i.source_status,
 md5('demo-user:'||b.owner)::uuid,md5('demo-user:'||b.owner)::uuid,now()-make_interval(days=>30+b.g),now()
FROM bulk b JOIN itinerary.itinerary_items i ON i.trip_id=md5('demo-trip:'||b.k)::uuid AND i.deleted_at IS NULL
ON CONFLICT(id) DO NOTHING;

-- Four records per derived trip create a substantial record gallery.
WITH ranked AS (
 SELECT i.*,row_number()OVER(PARTITION BY i.trip_id ORDER BY d.sort_order,i.sort_order)rn,d.date,
  t.owner_user_id FROM itinerary.itinerary_items i JOIN itinerary.itinerary_days d ON d.id=i.itinerary_day_id
 JOIN trip.trips t ON t.id=i.trip_id WHERE i.trip_id IN(SELECT md5('demo-bulk-trip:'||g)::uuid FROM generate_series(1,50)g)
 AND d.group_type = 'DAY' AND d.date IS NOT NULL
)
INSERT INTO record.trip_record_entries
 (id,trip_id,itinerary_day_id,itinerary_item_id,uploaded_by_user_id,title,caption,location_name,lat,lng,
 taken_at,visibility,status,created_at,updated_at)
SELECT md5('demo-bulk-record:'||id)::uuid,trip_id,itinerary_day_id,id,owner_user_id,
 CASE rn WHEN 1 THEN '여행의 첫 장면' WHEN 2 THEN '걷다가 멈춘 곳' WHEN 3 THEN '함께여서 좋았던 순간' ELSE '다시 보고 싶은 풍경' END,
 CASE rn%4 WHEN 0 THEN '다음 여행에도 꼭 다시 넣고 싶은 장소.' WHEN 1 THEN '아침 일찍 가니 한적해서 천천히 둘러볼 수 있었다.'
  WHEN 2 THEN '근처 골목까지 걸으니 예상보다 볼거리가 많았다.' ELSE '일정 사이에 여유를 둔 덕분에 오래 머물렀다.' END,
 place_name,lat,lng,date::timestamptz+make_interval(hours=>(9+rn*2)::int),'TRIP_MEMBERS','ACTIVE',
 date::timestamptz+make_interval(hours=>(9+rn*2)::int),now()
FROM ranked WHERE rn<=4 ON CONFLICT(id) DO NOTHING;

INSERT INTO media.media_files
 (id,owner_user_id,bucket,object_key,public_url,mime_type,byte_size,width,height,linked_resource_type,linked_resource_id,status,created_at)
SELECT md5('demo-bulk-record-media:'||r.id)::uuid,r.uploaded_by_user_id,'soomgil-local',
 'demo/bulk-records/'||r.id||'.jpg','https://picsum.photos/seed/bulk-record-'||replace(r.id::text,'-','')||'/1200/900',
 'image/jpeg',480000,1200,900,'TRIP_RECORD',r.id,'ACTIVE',r.created_at
FROM record.trip_record_entries r WHERE r.id IN(SELECT md5('demo-bulk-record:'||i.id)::uuid FROM itinerary.itinerary_items i)
ON CONFLICT(id) DO NOTHING;

INSERT INTO record.trip_record_media(record_entry_id,media_file_id,sort_order,caption,created_at)
SELECT r.id,md5('demo-bulk-record-media:'||r.id)::uuid,0,'여행자가 직접 남긴 기록',r.created_at
FROM record.trip_record_entries r WHERE r.id IN(SELECT md5('demo-bulk-record:'||i.id)::uuid FROM itinerary.itinerary_items i)
ON CONFLICT DO NOTHING;

-- Fifty more feed posts backed by real itineraries.
INSERT INTO community.posts
 (id,source_trip_id,source_trip_version,published_by_user_id,visibility,title,summary,snapshot_version,
 moderation_status,published_at,created_at,updated_at,snapshot)
SELECT md5('demo-bulk-post:'||g)::uuid,t.id,t.itinerary_version,t.owner_user_id,'PUBLIC',
 CASE g%6 WHEN 0 THEN '현지인이 알려준 동선으로 다녀온 ' WHEN 1 THEN '사진이 잘 나온 '
  WHEN 2 THEN '친구들과 웃다가 하루가 끝난 ' WHEN 3 THEN '대중교통만으로 충분했던 '
  WHEN 4 THEN '느긋하게 걷기 좋았던 ' ELSE '저장해두고 그대로 다녀온 ' END||t.display_destination||' 여행',
 CASE g%5 WHEN 0 THEN '실제로 걸어본 순서대로 일정과 이동 팁을 정리했습니다. 장소마다 머문 시간도 넉넉하게 잡았어요.'
  WHEN 1 THEN '유명한 곳만 빠르게 도는 대신 한 동네를 오래 걸었습니다. 근처에 함께 보기 좋은 장소도 담았어요.'
  WHEN 2 THEN '친구들과 취향 투표로 고른 장소들입니다. 다음 여행을 위해 리트립해 자유롭게 바꿔보세요.'
  WHEN 3 THEN '비용과 이동 시간을 줄이면서도 사진과 기록이 많이 남은 코스예요.'
  ELSE '서울과 대전을 자주 오가는 여행자가 추천하는 주말 코스입니다.' END,
 1,'VISIBLE',now()-make_interval(days=>1+(g*4)%150),now()-make_interval(days=>1+(g*4)%150),
 now()-make_interval(days=>(g*4)%150),'{"days":[],"routes":[],"authorDisplay":null}'::jsonb
FROM generate_series(1,50)g JOIN trip.trips t ON t.id=md5('demo-bulk-trip:'||g)::uuid
ON CONFLICT(id) DO NOTHING;

UPDATE community.posts p SET snapshot=jsonb_build_object(
 'days',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',d.id,'tripId',d.trip_id,'groupType',d.group_type,
  'dayNumber',d.day_number,'date',d.date,'title',d.title,'sortOrder',d.sort_order,
  'items',COALESCE((SELECT jsonb_agg(jsonb_build_object('id',i.id,'itineraryDayId',i.itinerary_day_id,
   'sortOrder',i.sort_order,'itemType',i.item_type,'placeRef',jsonb_build_object('provider',i.place_provider,
   'externalPlaceId',i.external_place_id),'placeName',i.place_name,'address',i.address,'lat',i.lat,'lng',i.lng,
   'thumbnailUrl',i.thumbnail_url,'sourceStatus',i.source_status)ORDER BY i.sort_order)
   FROM itinerary.itinerary_items i WHERE i.itinerary_day_id=d.id AND i.deleted_at IS NULL),'[]'::jsonb))ORDER BY d.sort_order)
   FROM itinerary.itinerary_days d WHERE d.trip_id=p.source_trip_id),'[]'::jsonb),
 'routes','[]'::jsonb,
 'authorDisplay',(SELECT jsonb_build_object('id',u.user_id,'displayName',u.display_name,'profileImageUrl',u.profile_image_url)
  FROM auth.user_profiles u WHERE u.user_id=p.published_by_user_id))
WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g);

INSERT INTO media.media_files
 (id,owner_user_id,bucket,object_key,public_url,mime_type,byte_size,width,height,linked_resource_type,linked_resource_id,status,created_at)
SELECT md5('demo-bulk-post-media:'||p.id)::uuid,p.published_by_user_id,'soomgil-local','demo/bulk-community/'||p.id||'.jpg',
 'https://picsum.photos/seed/bulk-community-'||replace(p.id::text,'-','')||'/1200/800','image/jpeg',560000,1200,800,
 'COMMUNITY_POST',p.id,'ACTIVE',p.published_at
FROM community.posts p WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g)
ON CONFLICT(id) DO NOTHING;

UPDATE community.posts p SET cover_media_file_id=md5('demo-bulk-post-media:'||p.id)::uuid
WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g);

INSERT INTO community.post_media(id,post_id,media_file_id,sort_order,caption,created_at)
SELECT md5('demo-bulk-post-media-link:'||p.id)::uuid,p.id,md5('demo-bulk-post-media:'||p.id)::uuid,0,
 '여행 대표 사진',p.published_at FROM community.posts p
WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g)
ON CONFLICT DO NOTHING;

-- Every bulk post receives three deterministic hashtags.
INSERT INTO community.post_hashtags(post_id,hashtag_id)
SELECT p.id,h.id FROM community.posts p
CROSS JOIN LATERAL(SELECT id FROM community.hashtags ORDER BY md5(p.id::text||id::text) LIMIT 3)h
WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g)
ON CONFLICT DO NOTHING;

UPDATE community.hashtags h SET usage_count=(SELECT count(*) FROM community.post_hashtags ph WHERE ph.hashtag_id=h.id);

INSERT INTO community.post_likes(post_id,user_id,created_at)
SELECT p.id,md5('demo-user:'||u)::uuid,p.published_at+make_interval(hours=>1+(u%72))
FROM community.posts p CROSS JOIN generate_series(1,120)u
WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g)
AND (u+get_byte(decode(md5(p.id::text),'hex'),0))%4<>0 AND md5('demo-user:'||u)::uuid<>p.published_by_user_id
ON CONFLICT DO NOTHING;

WITH texts AS (SELECT ARRAY[
 '동선이 현실적이라 그대로 따라가 보기 좋겠어요. 저장합니다!',
 '이 장소는 오전과 오후 중 언제가 덜 붐비나요?',
 '사진 분위기가 정말 좋네요. 다음 주말 후보로 찜했어요.',
 '대중교통 이동 시간까지 알려주셔서 도움이 됐어요.',
 '저도 다녀왔는데 근처 골목을 함께 보면 더 좋더라고요.',
 '리트립해서 친구들과 장소 두 곳만 바꿔봤어요.',
 '아이와 가도 무리 없는 코스인지 궁금합니다.',
 '비 오는 날 대체할 실내 장소도 있어서 좋네요.'
 ] arr)
INSERT INTO community.post_comments(id,post_id,author_user_id,content,depth,moderation_status,created_at,updated_at)
SELECT md5('demo-bulk-comment:'||p.id||':'||c)::uuid,p.id,md5('demo-user:'||(1+((c*17+g*11)%120)))::uuid,
 texts.arr[c],0,'VISIBLE',p.published_at+make_interval(hours=>c*9+g%8),p.published_at+make_interval(hours=>c*9+g%8)
FROM generate_series(1,50)g JOIN community.posts p ON p.id=md5('demo-bulk-post:'||g)::uuid
CROSS JOIN generate_series(1,8)c CROSS JOIN texts ON CONFLICT(id) DO NOTHING;

-- Twenty additional retrips make provenance/count filters meaningful.
INSERT INTO trip.trips(id,owner_user_id,title,display_destination,status,itinerary_version,retripped_from_post_id,
 retripped_from_snapshot_version,created_at,updated_at)
SELECT md5('demo-bulk-retrip:'||g)::uuid,md5('demo-user:'||(21+((g*19)%100)))::uuid,
 '리트립으로 다시 짠 '||p.title,source_trip.display_destination,'ACTIVE',1,p.id,1,
 now()-make_interval(days=>g%25),now()
FROM generate_series(1,20)g JOIN community.posts p ON p.id=md5('demo-bulk-post:'||g)::uuid
JOIN trip.trips source_trip ON source_trip.id=p.source_trip_id
ON CONFLICT(id) DO NOTHING;

INSERT INTO trip.trip_members(id,trip_id,user_id,role,status,joined_at)
SELECT md5('demo-bulk-retrip-member:'||g)::uuid,md5('demo-bulk-retrip:'||g)::uuid,
 md5('demo-user:'||(21+((g*19)%100)))::uuid,'MEMBER','ACTIVE',now()-make_interval(days=>g%25)
FROM generate_series(1,20)g ON CONFLICT(trip_id,user_id) DO NOTHING;

INSERT INTO itinerary.itinerary_days(id,trip_id,group_type,day_number,date,title,sort_order,created_at,updated_at)
SELECT md5('demo-bulk-retrip-day:'||g||':'||d.id)::uuid,md5('demo-bulk-retrip:'||g)::uuid,
 d.group_type,d.day_number,current_date+14+d.day_number,d.title,d.sort_order,now()-make_interval(days=>g%25),now()
FROM generate_series(1,20)g
JOIN community.posts p ON p.id=md5('demo-bulk-post:'||g)::uuid
JOIN itinerary.itinerary_days d ON d.trip_id=p.source_trip_id
ON CONFLICT(id) DO NOTHING;

INSERT INTO itinerary.itinerary_items
 (id,trip_id,itinerary_day_id,sort_order,item_type,place_provider,external_place_id,place_name,address,lat,lng,
  thumbnail_url,source_status,created_by_user_id,updated_by_user_id,created_at,updated_at)
SELECT md5('demo-bulk-retrip-item:'||g||':'||i.id)::uuid,md5('demo-bulk-retrip:'||g)::uuid,
 md5('demo-bulk-retrip-day:'||g||':'||i.itinerary_day_id)::uuid,i.sort_order,i.item_type,i.place_provider,
 i.external_place_id,i.place_name,i.address,i.lat,i.lng,i.thumbnail_url,i.source_status,
 md5('demo-user:'||(21+((g*19)%100)))::uuid,md5('demo-user:'||(21+((g*19)%100)))::uuid,
 now()-make_interval(days=>g%25),now()
FROM generate_series(1,20)g
JOIN community.posts p ON p.id=md5('demo-bulk-post:'||g)::uuid
JOIN itinerary.itinerary_items i ON i.trip_id=p.source_trip_id AND i.deleted_at IS NULL
ON CONFLICT(id) DO NOTHING;

INSERT INTO community.post_retrips(id,post_id,user_id,new_trip_id,snapshot_version,created_at)
SELECT md5('demo-bulk-retrip-link:'||g)::uuid,md5('demo-bulk-post:'||g)::uuid,
 md5('demo-user:'||(21+((g*19)%100)))::uuid,md5('demo-bulk-retrip:'||g)::uuid,1,now()-make_interval(days=>g%25)
FROM generate_series(1,20)g ON CONFLICT DO NOTHING;

-- Recalculate preference weights after the dense activity expansion.
INSERT INTO preference.user_preference_tag_weights
 (user_id,tag_id,positive_evidence,negative_evidence,preference_score,like_count,super_like_count,nope_count,updated_at)
SELECT r.user_id,t.tag_id,
 sum(CASE r.reaction WHEN 'LIKE' THEN t.weight WHEN 'SUPER_LIKE' THEN t.weight*1.7 ELSE 0 END),
 sum(CASE r.reaction WHEN 'NOPE' THEN t.weight ELSE 0 END),
 greatest(0.05,least(0.95,0.5+(sum(CASE WHEN r.reaction IN('LIKE','SUPER_LIKE') THEN t.weight ELSE 0 END)
  -sum(CASE WHEN r.reaction='NOPE' THEN t.weight ELSE 0 END))/25.0)),
 count(*)FILTER(WHERE r.reaction='LIKE'),count(*)FILTER(WHERE r.reaction='SUPER_LIKE'),
 count(*)FILTER(WHERE r.reaction='NOPE'),now()
FROM preference.user_place_reactions r
JOIN preference.place_tag_enrichment_tags t ON t.enrichment_id=r.place_tag_enrichment_id
GROUP BY r.user_id,t.tag_id
ON CONFLICT(user_id,tag_id) DO UPDATE SET positive_evidence=EXCLUDED.positive_evidence,
 negative_evidence=EXCLUDED.negative_evidence,preference_score=EXCLUDED.preference_score,
 like_count=EXCLUDED.like_count,super_like_count=EXCLUDED.super_like_count,nope_count=EXCLUDED.nope_count,updated_at=now();

-- A representative notification stream without exploding the table size.
INSERT INTO notification.notifications
 (id,recipient_user_id,actor_user_id,trip_id,type,title,body,payload,read_at,created_at)
SELECT md5('demo-bulk-notification:'||p.id||':'||u)::uuid,p.published_by_user_id,md5('demo-user:'||u)::uuid,
 p.source_trip_id,'COMMUNITY_POST_LIKED','게시물 반응이 늘고 있어요','새로운 사용자가 회원님의 여행을 좋아했습니다.',
 jsonb_build_object('postId',p.id),CASE WHEN u%3=0 THEN p.published_at+make_interval(hours=>u) END,
 p.published_at+make_interval(hours=>u)
FROM community.posts p CROSS JOIN generate_series(1,120)u
WHERE p.id IN(SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g) AND u%13=0
ON CONFLICT(id) DO NOTHING;

COMMIT;

-- Quick density report shown by psql after a successful load.
SELECT 'users' AS dataset, count(*) AS rows FROM auth.users WHERE id IN (SELECT md5('demo-user:'||n)::uuid FROM generate_series(1,120)n)
UNION ALL SELECT 'places',count(*) FROM tourism_source.attractions WHERE content_id BETWEEN 10001 AND 10040 OR content_id BETWEEN 20001 AND 20028
UNION ALL SELECT 'trips',count(*) FROM trip.trips WHERE display_destination LIKE '서울%' OR display_destination LIKE '대전%'
UNION ALL SELECT 'itinerary_items',count(*) FROM itinerary.itinerary_items WHERE place_provider='KTO' AND
 external_place_id ~ '^[0-9]+$' AND
 (external_place_id::int BETWEEN 10001 AND 10040 OR external_place_id::int BETWEEN 20001 AND 20028)
UNION ALL SELECT 'records',count(*) FROM record.trip_record_entries WHERE id IN (
 SELECT md5('demo-record:'||i.id)::uuid FROM itinerary.itinerary_items i UNION ALL
 SELECT md5('demo-bulk-record:'||i.id)::uuid FROM itinerary.itinerary_items i)
UNION ALL SELECT 'community_posts',count(*) FROM community.posts WHERE id IN (
 SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g UNION ALL
 SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post'))x(k))
UNION ALL SELECT 'likes',count(*) FROM community.post_likes WHERE post_id IN (
 SELECT md5('demo-bulk-post:'||g)::uuid FROM generate_series(1,50)g UNION ALL
 SELECT md5('demo-post:'||k)::uuid FROM (VALUES ('palace-post'),('night-post'),('seongsu-post'),('science-post'),('bread-post'),('green-post'),('modern-post'),('family-post'),('autumn-post'))x(k))
UNION ALL SELECT 'retrips',count(*) FROM community.post_retrips WHERE id::text IS NOT NULL;
