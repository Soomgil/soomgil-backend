-- ============================================================
-- 05_itinerary.sql
-- itinerary.itinerary_days, itinerary.itinerary_items,
-- itinerary.trip_routes, itinerary.route_match_requests,
-- itinerary.map_drawings
-- ============================================================
-- Day pattern:    d{trip2}{day}0000-0000-4000-8000-0000000000NN
-- Item pattern:   e{trip2}{day}{item}000-0000-4000-8000-000000000NN
-- Route pattern:  f2000000-0000-4000-8000-00000000000N
-- ============================================================

BEGIN;

-- ============================================================
-- Trip 1: 제주도 3박 4일 감성 여행
-- ============================================================
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0101000-0000-4000-8000-000000000001','c0000000-0000-4000-8000-000000000001','DAY',1,'2026-07-01','제주시 도착 & 동쪽 드라이브',0),
  ('d0102000-0000-4000-8000-000000000002','c0000000-0000-4000-8000-000000000001','DAY',2,'2026-07-02','우도 & 성산일출봉',1),
  ('d0103000-0000-4000-8000-000000000003','c0000000-0000-4000-8000-000000000001','DAY',3,'2026-07-03','서귀포 남쪽 코스',2),
  ('d0104000-0000-4000-8000-000000000004','c0000000-0000-4000-8000-000000000001','DAY',4,'2026-07-04','한라산 등반 & 귀가',3),
  ('d0199000-0000-4000-8000-000000000099','c0000000-0000-4000-8000-000000000001','UNSCHEDULED',NULL,NULL,'일차 미정',4);

INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  -- Day 1
  ('e0101010-0000-4000-8000-000000000001','c0000000-0000-4000-8000-000000000001','d0101000-0000-4000-8000-000000000001',0,'PLACE','KTO','11897234','제주 공항','제주특별자치도 제주시 공항로 2', 33.5069, 126.4930,'https://cdn.soomgil.test/pl/jeju-airport.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000002'),
  ('e0101020-0000-4000-8000-000000000002','c0000000-0000-4000-8000-000000000001','d0101000-0000-4000-8000-000000000001',1,'PLACE','KTO','27534592','제주 동문시장','제주 제주시 특별자치도 동문로 20',33.5139,126.5400,'https://cdn.soomgil.test/pl/dongmun.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000002'),
  ('e0101030-0000-4000-8000-000000000003','c0000000-0000-4000-8000-000000000001','d0101000-0000-4000-8000-000000000001',2,'PLACE','KTO','8239472','까사 빛나는 오브렐','제주특별자치도 서귀포시 안덕면',33.2470,126.3500,'https://cdn.soomgil.test/pl/obrel.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000004'),
  -- Day 2
  ('e0102010-0000-4000-8000-000000000004','c0000000-0000-4000-8000-000000000001','d0102000-0000-4000-8000-000000000002',0,'PLACE','KTO','78923471','성산일출봉','제주특별자치도 서귀포시 성산읍',33.4584,126.9418,'https://cdn.soomgil.test/pl/seongsan.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000002'),
  ('e0102020-0000-4000-8000-000000000005','c0000000-0000-4000-8000-000000000001','d0102000-0000-4000-8000-000000000002',1,'PLACE','KTO','81234567','우도','제주특별자치도 제주시 우도면',33.4960,126.9520,'https://cdn.soomgil.test/pl/udo.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000007'),
  -- Day 3
  ('e0103010-0000-4000-8000-000000000006','c0000000-0000-4000-8000-000000000001','d0103000-0000-4000-8000-000000000003',0,'PLACE','KTO','26748591','정방폭포','제주특별자치도 서귀포시 칠십리로 214',33.2385,126.4030,'https://cdn.soomgil.test/pl/jeongbang.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000002'),
  ('e0103020-0000-4000-8000-000000000007','c0000000-0000-4000-8000-000000000001','d0103000-0000-4000-8000-000000000003',1,'PLACE','KTO','89712346','주상절리대','제주특별자치도 서귀포시 예래동',33.2340,126.4240,'https://cdn.soomgil.test/pl/jusang.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000004'),
  -- Day 4
  ('e0104010-0000-4000-8000-000000000008','c0000000-0000-4000-8000-000000000001','d0104000-0000-4000-8000-000000000004',0,'PLACE','KTO','24591827','한라산 국립공원','제주특별자치도 제주시 1100로',33.3617,126.5330,'https://cdn.soomgil.test/pl/halla.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000002'),
  -- Unscheduled
  ('e0199010-0000-4000-8000-000000000099','c0000000-0000-4000-8000-000000000001','d0199000-0000-4000-8000-000000000099',0,'CUSTOM_PLACE',NULL,NULL,'아직 날짜 미정 카페',NULL, 33.5000,126.5000,NULL,'AVAILABLE','a0000000-0000-4000-8000-000000000002');

-- Routes for Trip 1
INSERT INTO itinerary.trip_routes (id, trip_id, origin_itinerary_item_id, destination_itinerary_item_id, mode, provider, provider_profile, geometry, distance_meters, duration_seconds, confidence, created_by_user_id) VALUES
  ('f2000000-0000-4000-8000-000000000001','c0000000-0000-4000-8000-000000000001','e0101010-0000-4000-8000-000000000001','e0101020-0000-4000-8000-000000000002','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[126.4930,33.5069],[126.5400,33.5139]]}',5800,720,0.9800,'a0000000-0000-4000-8000-000000000002'),
  ('f2000000-0000-4000-8000-000000000002','c0000000-0000-4000-8000-000000000001','e0101020-0000-4000-8000-000000000002','e0101030-0000-4000-8000-000000000003','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[126.5400,33.5139],[126.3500,33.2470]]}',38200,2400,0.9500,'a0000000-0000-4000-8000-000000000002'),
  ('f2000000-0000-4000-8000-000000000003','c0000000-0000-4000-8000-000000000001','e0102010-0000-4000-8000-000000000004','e0102020-0000-4000-8000-000000000005','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[126.9418,33.4584],[126.9520,33.4960]]}',5200,600,0.9900,'a0000000-0000-4000-8000-000000000007'),
  ('f2000000-0000-4000-8000-000000000004','c0000000-0000-4000-8000-000000000001','e0103010-0000-4000-8000-000000000006','e0103020-0000-4000-8000-000000000007','WALKING','MAPBOX','mapbox/walking','{"type":"LineString","coordinates":[[126.4030,33.2385],[126.4240,33.2340]]}',2400,1800,0.8700,'a0000000-0000-4000-8000-000000000004');

-- Map drawings for Trip 1
INSERT INTO itinerary.map_drawings (id, trip_id, drawing_type, geometry, style, label, sort_order, version, created_by_user_id) VALUES
  ('4d000000-0000-4000-8000-000000000001','c0000000-0000-4000-8000-000000000001','MARKER',  '{"type":"Point","coordinates":[126.4930,33.5069]}','{"color":"#3b82f6"}','제주 공항',0,1,'a0000000-0000-4000-8000-000000000002'),
  ('4d000000-0000-4000-8000-000000000002','c0000000-0000-4000-8000-000000000001','FREEHAND','{"type":"LineString","coordinates":[[126.49,33.50],[126.54,33.51],[126.35,33.24]]}','{"color":"#ef4444","width":3}','드라이브 경로 메모',1,2,'a0000000-0000-4000-8000-000000000002'),
  ('4d000000-0000-4000-8000-000000000003','c0000000-0000-4000-8000-000000000001','TEXT',    '{"type":"Point","coordinates":[126.94,33.45]}','{"color":"#10b981","size":14}','일출봉 사진 스팟!',2,1,'a0000000-0000-4000-8000-000000000007');

-- ============================================================
-- Trip 2: 부산 해운대 가족여행
-- ============================================================
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0201000-0000-4000-8000-000000000011','c0000000-0000-4000-8000-000000000002','DAY',1,'2026-07-10','해운대 & 광안리',0),
  ('d0202000-0000-4000-8000-000000000012','c0000000-0000-4000-8000-000000000002','DAY',2,'2026-07-11','기장 해안 & 감천',1),
  ('d0203000-0000-4000-8000-000000000013','c0000000-0000-4000-8000-000000000002','DAY',3,'2026-07-12','태종대 & 자갈치',2);

INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0201010-0000-4000-8000-000000000011','c0000000-0000-4000-8000-000000000002','d0201000-0000-4000-8000-000000000011',0,'PLACE','KTO','72384729','해운대 해수욕장','부산광역시 해운대구 우동',35.1587,129.1604,'https://cdn.soomgil.test/pl/haeundae.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000003'),
  ('e0201020-0000-4000-8000-000000000012','c0000000-0000-4000-8000-000000000002','d0201000-0000-4000-8000-000000000011',1,'PLACE','KTO','21384710','광안리 해수욕장','부산광역시 수영구 광안해변로',35.1531,129.1186,'https://cdn.soomgil.test/pl/gwangalli.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000003'),
  ('e0201030-0000-4000-8000-000000000013','c0000000-0000-4000-8000-000000000002','d0201000-0000-4000-8000-000000000011',2,'PLACE','KTO','84729103','더베이 101','부산광역시 해운대구 동백로 52',35.1433,129.1594,'https://cdn.soomgil.test/pl/bay101.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000006'),
  ('e0202010-0000-4000-8000-000000000014','c0000000-0000-4000-8000-000000000002','d0202000-0000-4000-8000-000000000012',0,'PLACE','KTO','91827364','기장 동백섬','부산광역시 기장군 기장읍',35.2435,129.2217,'https://cdn.soomgil.test/pl/dongbaek.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000003'),
  ('e0202020-0000-4000-8000-000000000015','c0000000-0000-4000-8000-000000000002','d0202000-0000-4000-8000-000000000012',1,'PLACE','KTO','17263849','감천문화마을','부산광역시 사하구 감내2로 203',35.0966,129.0105,'https://cdn.soomgil.test/pl/gamcheon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000006'),
  ('e0203010-0000-4000-8000-000000000016','c0000000-0000-4000-8000-000000000002','d0203000-0000-4000-8000-000000000013',0,'PLACE','KTO','82736194','태종대','부산광역시 영도구 전망산로 209',35.0513,129.0860,'https://cdn.soomgil.test/pl/taejongdae.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000003'),
  ('e0203020-0000-4000-8000-000000000017','c0000000-0000-4000-8000-000000000002','d0203000-0000-4000-8000-000000000013',1,'PLACE','KTO','64738291','자갈치시장','부산광역시 중구 자갈치해안로 52',35.0966,129.0305,'https://cdn.soomgil.test/pl/jagalchi.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000010');

INSERT INTO itinerary.trip_routes (id, trip_id, origin_itinerary_item_id, destination_itinerary_item_id, mode, provider, provider_profile, geometry, distance_meters, duration_seconds, confidence, created_by_user_id) VALUES
  ('f2000000-0000-4000-8000-000000000010','c0000000-0000-4000-8000-000000000002','e0201010-0000-4000-8000-000000000011','e0201020-0000-4000-8000-000000000012','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[129.1604,35.1587],[129.1186,35.1531]]}',6200,840,0.9700,'a0000000-0000-4000-8000-000000000003'),
  ('f2000000-0000-4000-8000-000000000011','c0000000-0000-4000-8000-000000000002','e0202010-0000-4000-8000-000000000014','e0202020-0000-4000-8000-000000000015','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[129.2217,35.2435],[129.0105,35.0966]]}',28400,2100,0.9300,'a0000000-0000-4000-8000-000000000006');

-- ============================================================
-- Trip 3: 강릉 바다 카페 투어
-- ============================================================
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0301000-0000-4000-8000-000000000021','c0000000-0000-4000-8000-000000000003','DAY',1,'2026-08-01','경포대 & 안목 카페거리',0),
  ('d0302000-0000-4000-8000-000000000022','c0000000-0000-4000-8000-000000000003','DAY',2,'2026-08-02','정동진 & 강문해변',1);

INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0301010-0000-4000-8000-000000000021','c0000000-0000-4000-8000-000000000003','d0301000-0000-4000-8000-000000000021',0,'PLACE','KTO','18273645','안목 커피거리','강원특별자치도 강릉시 창해로14번길',37.7720,128.9470,'https://cdn.soomgil.test/pl/anmok.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000004'),
  ('e0301020-0000-4000-8000-000000000022','c0000000-0000-4000-8000-000000000003','d0301000-0000-4000-8000-000000000021',1,'PLACE','KTO','28374658','경포대','강원특별자치도 강릉시 경포로 365',37.7954,128.9087,'https://cdn.soomgil.test/pl/gyeongpo.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000007'),
  ('e0301030-0000-4000-8000-000000000023','c0000000-0000-4000-8000-000000000003','d0301000-0000-4000-8000-000000000021',2,'PLACE','KTO','38475869','토즈 커피','강원특별자치도 강릉시 안목해안로 106',37.7715,128.9465,'https://cdn.soomgil.test/pl/toz.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000004'),
  ('e0302010-0000-4000-8000-000000000024','c0000000-0000-4000-8000-000000000003','d0302000-0000-4000-8000-000000000022',0,'PLACE','KTO','48586970','정동진 역','강원특별자치도 강릉시 강동면 정동진리',37.6905,129.0288,'https://cdn.soomgil.test/pl/jeongdongjin.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000004'),
  ('e0302020-0000-4000-8000-000000000025','c0000000-0000-4000-8000-000000000003','d0302000-0000-4000-8000-000000000022',1,'PLACE','KTO','58697081','강문해변','강원특별자치도 강릉시 강문동',37.7836,128.9435,'https://cdn.soomgil.test/pl/gangmun.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000012');

INSERT INTO itinerary.trip_routes (id, trip_id, origin_itinerary_item_id, destination_itinerary_item_id, mode, provider, provider_profile, geometry, distance_meters, duration_seconds, confidence, created_by_user_id) VALUES
  ('f2000000-0000-4000-8000-000000000020','c0000000-0000-4000-8000-000000000003','e0301010-0000-4000-8000-000000000021','e0301020-0000-4000-8000-000000000022','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[128.9470,37.7720],[128.9087,37.7954]]}',5400,660,0.9600,'a0000000-0000-4000-8000-000000000004');

-- ============================================================
-- Trip 4: 경주 역사 탐방
-- ============================================================
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0401000-0000-4000-8000-000000000031','c0000000-0000-4000-8000-000000000004','DAY',1,'2026-09-01','불국사 & 석굴암',0),
  ('d0402000-0000-4000-8000-000000000032','c0000000-0000-4000-8000-000000000004','DAY',2,'2026-09-02','대릉원 & 첨성대',1),
  ('d0403000-0000-4000-8000-000000000033','c0000000-0000-4000-8000-000000000004','DAY',3,'2026-09-03','보문관광단지',2);

INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0401010-0000-4000-8000-000000000031','c0000000-0000-4000-8000-000000000004','d0401000-0000-4000-8000-000000000031',0,'PLACE','KTO','11223344','불국사','경상북도 경주시 불국로 385',35.7897,129.3323,'https://cdn.soomgil.test/pl/bulguksa.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000005'),
  ('e0401020-0000-4000-8000-000000000032','c0000000-0000-4000-8000-000000000004','d0401000-0000-4000-8000-000000000031',1,'PLACE','KTO','22334455','석굴암','경상북도 경주시 진현동',35.7949,129.3482,'https://cdn.soomgil.test/pl/seokguram.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000008'),
  ('e0402010-0000-4000-8000-000000000033','c0000000-0000-4000-8000-000000000004','d0402000-0000-4000-8000-000000000032',0,'PLACE','KTO','33445566','대릉원','경상북도 경주시 황남동',35.8360,129.2240,'https://cdn.soomgil.test/pl/daereungwon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000005'),
  ('e0402020-0000-4000-8000-000000000034','c0000000-0000-4000-8000-000000000004','d0402000-0000-4000-8000-000000000032',1,'PLACE','KTO','44556677','첨성대','경상북도 경주시 인왕동',35.8347,129.2190,'https://cdn.soomgil.test/pl/cheomseongdae.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000005'),
  ('e0403010-0000-4000-8000-000000000035','c0000000-0000-4000-8000-000000000004','d0403000-0000-4000-8000-000000000033',0,'PLACE','KTO','55667788','보문관광단지','경상북도 경주시 보문로',35.8393,129.3050,'https://cdn.soomgil.test/pl/bomun.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000008');

INSERT INTO itinerary.trip_routes (id, trip_id, origin_itinerary_item_id, destination_itinerary_item_id, mode, provider, provider_profile, geometry, distance_meters, duration_seconds, confidence, created_by_user_id) VALUES
  ('f2000000-0000-4000-8000-000000000030','c0000000-0000-4000-8000-000000000004','e0401010-0000-4000-8000-000000000031','e0401020-0000-4000-8000-000000000032','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[129.3323,35.7897],[129.3482,35.7949]]}',3200,480,0.9800,'a0000000-0000-4000-8000-000000000005');

-- ============================================================
-- Trip 5: 여수 밤바다 로맨틱
-- ============================================================
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0501000-0000-4000-8000-000000000041','c0000000-0000-4000-8000-000000000005','DAY',1,'2026-08-15','여수 엑스포 & 케이블카',0),
  ('d0502000-0000-4000-8000-000000000042','c0000000-0000-4000-8000-000000000005','DAY',2,'2026-08-16','돌산도 & 향일암',1);

INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0501010-0000-4000-8000-000000000041','c0000000-0000-4000-8000-000000000005','d0501000-0000-4000-8000-000000000041',0,'PLACE','KTO','77889910','여수 엑스포공원','전라남도 여수시 엑스포대로 1',34.7950,127.5530,'https://cdn.soomgil.test/pl/expo.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000006'),
  ('e0501020-0000-4000-8000-000000000042','c0000000-0000-4000-8000-000000000005','d0501000-0000-4000-8000-000000000041',1,'PLACE','KTO','88990011','여수 해상 케이블카','전라남도 여수시 돌산읍',34.7700,127.5450,'https://cdn.soomgil.test/pl/cablecar.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000006'),
  ('e0502010-0000-4000-8000-000000000043','c0000000-0000-4000-8000-000000000005','d0502000-0000-4000-8000-000000000042',0,'PLACE','KTO','99001122','향일암','전라남도 여수시 돌산읍',34.7350,127.5100,'https://cdn.soomgil.test/pl/hyangilam.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000013');

INSERT INTO itinerary.trip_routes (id, trip_id, origin_itinerary_item_id, destination_itinerary_item_id, mode, provider, provider_profile, geometry, distance_meters, duration_seconds, confidence, created_by_user_id) VALUES
  ('f2000000-0000-4000-8000-000000000040','c0000000-0000-4000-8000-000000000005','e0501010-0000-4000-8000-000000000041','e0501020-0000-4000-8000-000000000042','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[127.5530,34.7950],[127.5450,34.7700]]}',3400,420,0.9500,'a0000000-0000-4000-8000-000000000006');

-- ============================================================
-- Trip 6: 속초·양양 서핑
-- ============================================================
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0601000-0000-4000-8000-000000000051','c0000000-0000-4000-8000-000000000006','DAY',1,'2026-08-20','양양 서핑',0),
  ('d0602000-0000-4000-8000-000000000052','c0000000-0000-4000-8000-000000000006','DAY',2,'2026-08-21','속초 시장 & 설악산',1);

INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0601010-0000-4000-8000-000000000051','c0000000-0000-4000-8000-000000000006','d0601000-0000-4000-8000-000000000051',0,'PLACE','KTO','12131415','양양 해수욕장','강원특별자치도 양양군 현남면',38.0760,128.6360,'https://cdn.soomgil.test/pl/yangyang.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000007'),
  ('e0601020-0000-4000-8000-000000000052','c0000000-0000-4000-8000-000000000006','d0601000-0000-4000-8000-000000000051',1,'PLACE','KTO','13141516','서피비치 양양','강원특별자치도 양양군 현남면 인구리',38.0730,128.6380,'https://cdn.soomgil.test/pl/surfbeach.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000007'),
  ('e0602010-0000-4000-8000-000000000053','c0000000-0000-4000-8000-000000000006','d0602000-0000-4000-8000-000000000052',0,'PLACE','KTO','14151617','속초 중앙시장','강원특별자치도 속초시 중앙로',38.2075,128.5908,'https://cdn.soomgil.test/pl/sokcho.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000009'),
  ('e0602020-0000-4000-8000-000000000054','c0000000-0000-4000-8000-000000000006','d0602000-0000-4000-8000-000000000052',1,'PLACE','KTO','15161718','설악산 국립공원','강원특별자치도 속초시 설악산로',38.1180,128.4470,'https://cdn.soomgil.test/pl/seorak.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000011');

INSERT INTO itinerary.trip_routes (id, trip_id, origin_itinerary_item_id, destination_itinerary_item_id, mode, provider, provider_profile, geometry, distance_meters, duration_seconds, confidence, created_by_user_id) VALUES
  ('f2000000-0000-4000-8000-000000000050','c0000000-0000-4000-8000-000000000006','e0601010-0000-4000-8000-000000000051','e0601020-0000-4000-8000-000000000052','WALKING','MAPBOX','mapbox/walking','{"type":"LineString","coordinates":[[128.6360,38.0760],[128.6380,38.0730]]}',400,300,0.9900,'a0000000-0000-4000-8000-000000000007'),
  ('f2000000-0000-4000-8000-000000000051','c0000000-0000-4000-8000-000000000006','e0602010-0000-4000-8000-000000000053','e0602020-0000-4000-8000-000000000054','DRIVING','MAPBOX','mapbox/driving','{"type":"LineString","coordinates":[[128.5908,38.2075],[128.4470,38.1180]]}',19000,1500,0.9400,'a0000000-0000-4000-8000-000000000011');

-- ============================================================
-- Trip 7-22: Lighter itinerary (1-2 items each)
-- ============================================================

-- Trip 7: 전주 한옥마을 미식
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0701000-0000-4000-8000-000000000061','c0000000-0000-4000-8000-000000000007','DAY',1,'2026-10-01','한옥마을 & 비빔밥',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0701010-0000-4000-8000-000000000061','c0000000-0000-4000-8000-000000000007','d0701000-0000-4000-8000-000000000061',0,'PLACE','KTO','16171819','전주 한옥마을','전북특별자치도 전주시 완산구 태조로',35.8150,127.1490,'https://cdn.soomgil.test/pl/hanok.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000008'),
  ('e0701020-0000-4000-8000-000000000062','c0000000-0000-4000-8000-000000000007','d0701000-0000-4000-8000-000000000061',1,'PLACE','KTO','17181920','전주 비빔밥','전북특별자치도 전주시 완산구',35.8120,127.1500,'https://cdn.soomgil.test/pl/bibimbap.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000014');

-- Trip 8: 거제·통영
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0801000-0000-4000-8000-000000000071','c0000000-0000-4000-8000-000000000008','DAY',1,'2026-07-25','외도 & 해금강',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0801010-0000-4000-8000-000000000071','c0000000-0000-4000-8000-000000000008','d0801000-0000-4000-8000-000000000071',0,'PLACE','KTO','18192021','외도 보타니아','경상남도 거제시 일운면',34.7980,128.7050,'https://cdn.soomgil.test/pl/oedo.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000009'),
  ('e0801020-0000-4000-8000-000000000072','c0000000-0000-4000-8000-000000000008','d0801000-0000-4000-8000-000000000071',1,'PLACE','KTO','19202122','통영 한산도','경상남도 통영시 한산면',34.7520,128.4500,'https://cdn.soomgil.test/pl/hansan.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000015');

-- Trip 9: 서울 도심 힐링
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d0901000-0000-4000-8000-000000000081','c0000000-0000-4000-8000-000000000009','DAY',1,'2026-06-20','북촌 & 성곡길',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e0901010-0000-4000-8000-000000000081','c0000000-0000-4000-8000-000000000009','d0901000-0000-4000-8000-000000000081',0,'PLACE','KTO','20212223','북촌 한옥마을','서울특별시 종로구 계동',37.5815,126.9850,'https://cdn.soomgil.test/pl/bukchon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000010'),
  ('e0901020-0000-4000-8000-000000000082','c0000000-0000-4000-8000-000000000009','d0901000-0000-4000-8000-000000000081',1,'PLACE','KTO','21222324','성수동 카페거리','서울특별시 성동구 성수동',37.5440,127.0560,'https://cdn.soomgil.test/pl/seongsu.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000017');

-- Trip 10: 평창 스키
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1001000-0000-4000-8000-000000000091','c0000000-0000-4000-8000-000000000010','DAY',1,'2026-01-10','용평스키장',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1001010-0000-4000-8000-000000000091','c0000000-0000-4000-8000-000000000010','d1001000-0000-4000-8000-000000000091',0,'PLACE','KTO','22232425','용평리조트','강원특별자치도 평창군 대관령면',37.6425,128.6650,'https://cdn.soomgil.test/pl/yongpyong.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000011'),
  ('e1001020-0000-4000-8000-000000000092','c0000000-0000-4000-8000-000000000010','d1001000-0000-4000-8000-000000000091',1,'PLACE','KTO','23242526','대관령 양떼목장','강원특별자치도 평창군 대관령면',37.6880,128.7180,'https://cdn.soomgil.test/pl/sheep.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000018');

-- Trip 11: 제주 워케이션
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1101000-0000-4000-8000-000000000101','c0000000-0000-4000-8000-000000000011','DAY',1,'2026-09-10','서귀포 카페 워케이션',0),
  ('d1102000-0000-4000-8000-000000000102','c0000000-0000-4000-8000-000000000011','DAY',2,'2026-09-11','오설록 & 산방산',1);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1101010-0000-4000-8000-000000000101','c0000000-0000-4000-8000-000000000011','d1101000-0000-4000-8000-000000000101',0,'PLACE','KTO','24252627','오설록 뮤지엄','제주특별자치도 서귀포시 안덕면',33.2980,126.2890,'https://cdn.soomgil.test/pl/osulloc.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000012'),
  ('e1101020-0000-4000-8000-000000000102','c0000000-0000-4000-8000-000000000011','d1101000-0000-4000-8000-000000000101',1,'PLACE','KTO','25262728','산방산','제주특별자치도 서귀포시 산방로',33.2410,126.3170,'https://cdn.soomgil.test/pl/sanbang.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000012'),
  ('e1102010-0000-4000-8000-000000000103','c0000000-0000-4000-8000-000000000011','d1102000-0000-4000-8000-000000000102',0,'PLACE','KTO','26272829','중문 색달해수욕장','제주특별자치도 서귀포시 색달동',33.2470,126.4110,'https://cdn.soomgil.test/pl/jungmun.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000012');

-- Trip 12: 담양
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1201000-0000-4000-8000-000000000111','c0000000-0000-4000-8000-000000000012','DAY',1,'2026-06-15','죽녹원 & 메타세쿼이아',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1201010-0000-4000-8000-000000000111','c0000000-0000-4000-8000-000000000012','d1201000-0000-4000-8000-000000000111',0,'PLACE','KTO','27282930','죽녹원','전라남도 담양군 담양읍',35.3190,126.9890,'https://cdn.soomgil.test/pl/juknokwon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000013'),
  ('e1201020-0000-4000-8000-000000000112','c0000000-0000-4000-8000-000000000012','d1201000-0000-4000-8000-000000000111',1,'PLACE','KTO','28293031','메타세쿼이아 가로수길','전라남도 담양군 담양읍',35.3170,126.9950,'https://cdn.soomgil.test/pl/meta.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000019');

-- Trip 13: 순천만
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1301000-0000-4000-8000-000000000121','c0000000-0000-4000-8000-000000000013','DAY',1,'2026-10-20','순천만 습지 & 숲',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1301010-0000-4000-8000-000000000121','c0000000-0000-4000-8000-000000000013','d1301000-0000-4000-8000-000000000121',0,'PLACE','KTO','29303132','순천만 국가정원','전라남도 순천시 국가정원로',34.9500,127.4980,'https://cdn.soomgil.test/pl/suncheon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000014');

-- Trip 14: 가평
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1401000-0000-4000-8000-000000000131','c0000000-0000-4000-8000-000000000014','DAY',1,'2026-06-22','청평호수 & 아침고요',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1401010-0000-4000-8000-000000000131','c0000000-0000-4000-8000-000000000014','d1401000-0000-4000-8000-000000000131',0,'PLACE','KTO','30313233','청평호수','경기도 가평군 청평면',37.7270,127.4290,'https://cdn.soomgil.test/pl/cheongpyeong.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000015');

-- Trip 15: 부산 송정 카페
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1501000-0000-4000-8000-000000000141','c0000000-0000-4000-8000-000000000015','DAY',1,'2026-08-05','송정 카페 & 다대포',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1501010-0000-4000-8000-000000000141','c0000000-0000-4000-8000-000000000015','d1501000-0000-4000-8000-000000000141',0,'PLACE','KTO','31323334','송정 해수욕장','부산광역시 기장군 송정동',35.1820,129.2000,'https://cdn.soomgil.test/pl/songjeong.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000001');

-- Trip 16: 안동
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1601000-0000-4000-8000-000000000151','c0000000-0000-4000-8000-000000000016','DAY',1,'2026-09-25','하회마을 & 도산서원',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1601010-0000-4000-8000-000000000151','c0000000-0000-4000-8000-000000000016','d1601000-0000-4000-8000-000000000151',0,'PLACE','KTO','32333435','하회마을','경상북도 안동시 풍천면',36.5340,128.5180,'https://cdn.soomgil.test/pl/hahoe.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000017');

-- Trip 17: 보령
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1701000-0000-4000-8000-000000000161','c0000000-0000-4000-8000-000000000017','DAY',1,'2026-07-15','머드축제 & 대천해수욕장',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1701010-0000-4000-8000-000000000161','c0000000-0000-4000-8000-000000000017','d1701000-0000-4000-8000-000000000161',0,'PLACE','KTO','33343536','대천해수욕장','충청남도 보령시 신흥동',36.3230,126.5140,'https://cdn.soomgil.test/pl/daecheon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000018');

-- Trip 18: 태안
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1801000-0000-4000-8000-000000000171','c0000000-0000-4000-8000-000000000018','DAY',1,'2026-10-10','안면도 해변',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1801010-0000-4000-8000-000000000171','c0000000-0000-4000-8000-000000000018','d1801000-0000-4000-8000-000000000171',0,'PLACE','KTO','34353637','안면도 꽃지해수욕장','충청남도 태안군 안면읍',36.5570,126.3360,'https://cdn.soomgil.test/pl/anmyeon.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000019');

-- Trip 19: 수원
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d1901000-0000-4000-8000-000000000181','c0000000-0000-4000-8000-000000000019','DAY',1,'2026-06-25','수원 화성 산책',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e1901010-0000-4000-8000-000000000181','c0000000-0000-4000-8000-000000000019','d1901000-0000-4000-8000-000000000181',0,'PLACE','KTO','35363738','수원 화성','경기도 수원시 장안구',37.2850,127.0180,'https://cdn.soomgil.test/pl/hwaseong.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000020');

-- Trip 20: 제주 동쪽
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d2001000-0000-4000-8000-000000000191','c0000000-0000-4000-8000-000000000020','DAY',1,'2026-08-28','함덕 & 김녕',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e2001010-0000-4000-8000-000000000191','c0000000-0000-4000-8000-000000000020','d2001000-0000-4000-8000-000000000191',0,'PLACE','KTO','36373839','함덕 서귀포 카페거리','제주특별자치도 제주시 조천읍',33.5420,126.6710,'https://cdn.soomgil.test/pl/hamdok.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000002'),
  ('e2001020-0000-4000-8000-000000000192','c0000000-0000-4000-8000-000000000020','d2001000-0000-4000-8000-000000000191',1,'PLACE','KTO','37383940','김녕 만굴굴','제주특별자치도 제주시 구좌읍',33.5180,126.7710,'https://cdn.soomgil.test/pl/gimnyeong.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000005');

-- Trip 21: 강릉 커피축제
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d2101000-0000-4000-8000-000000000201','c0000000-0000-4000-8000-000000000021','DAY',1,'2026-10-05','강릉 커피축제',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e2101010-0000-4000-8000-000000000201','c0000000-0000-4000-8000-000000000021','d2101000-0000-4000-8000-000000000201',0,'PLACE','KTO','38394041','강릉 커피축제장','강원특별자치도 강릉시 경포로',37.7950,128.9080,'https://cdn.soomgil.test/pl/coffeefest.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000004');

-- Trip 22: 고창
INSERT INTO itinerary.itinerary_days (id, trip_id, group_type, day_number, date, title, sort_order) VALUES
  ('d2201000-0000-4000-8000-000000000211','c0000000-0000-4000-8000-000000000022','DAY',1,'2026-05-20','고창 보리밭 축제',0);
INSERT INTO itinerary.itinerary_items (id, trip_id, itinerary_day_id, sort_order, item_type, place_provider, external_place_id, place_name, address, lat, lng, thumbnail_url, source_status, created_by_user_id) VALUES
  ('e2201010-0000-4000-8000-000000000211','c0000000-0000-4000-8000-000000000022','d2201000-0000-4000-8000-000000000211',0,'PLACE','KTO','39404142','고창 보리밭축제','전북특별자치도 고창군 고창읍',35.4350,126.7020,'https://cdn.soomgil.test/pl/gochang.jpg','AVAILABLE','a0000000-0000-4000-8000-000000000013');

-- ============================================================
-- route_match_requests (sample)
-- ============================================================
INSERT INTO itinerary.route_match_requests (trip_id, trip_route_id, origin_itinerary_item_id, destination_itinerary_item_id, requested_by_user_id, provider, provider_profile, input_coordinates, radiuses, tidy, request_hash, status, confidence, distance_meters, duration_seconds, tracepoints, matchings_metadata, error_code, error_message, created_at, completed_at) VALUES
  ('c0000000-0000-4000-8000-000000000001','f2000000-0000-4000-8000-000000000001','e0101010-0000-4000-8000-000000000001','e0101020-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000002','MAPBOX','mapbox/driving','[[126.4930,33.5069],[126.5400,33.5139]]','[15,15]',false,'hash001','SUCCEEDED',0.9800,5800,720,'{}','{"confidence":0.98}',NULL,NULL,'2026-06-01 12:00:00+09','2026-06-01 12:00:05+09'),
  ('c0000000-0000-4000-8000-000000000001','f2000000-0000-4000-8000-000000000002','e0101020-0000-4000-8000-000000000002','e0101030-0000-4000-8000-000000000003','a0000000-0000-4000-8000-000000000002','MAPBOX','mapbox/driving','[[126.5400,33.5139],[126.3500,33.2470]]','[25,25]',false,'hash002','SUCCEEDED',0.9500,38200,2400,'{}','{"confidence":0.95}',NULL,NULL,'2026-06-01 14:00:00+09','2026-06-01 14:00:08+09'),
  ('c0000000-0000-4000-8000-000000000006','f2000000-0000-4000-8000-000000000050','e0601010-0000-4000-8000-000000000051','e0601020-0000-4000-8000-000000000052','a0000000-0000-4000-8000-000000000007','MAPBOX','mapbox/walking','[[128.6360,38.0760],[128.6380,38.0730]]','[10,10]',false,'hash003','SUCCEEDED',0.9900,400,300,'{}','{"confidence":0.99}',NULL,NULL,'2026-06-02 09:00:00+09','2026-06-02 09:00:03+09');

COMMIT;
