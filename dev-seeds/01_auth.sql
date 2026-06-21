-- ============================================================
-- 01_auth.sql  (DBML 재정렬 버전)
-- auth.users / user_email_addresses / user_profiles / user_settings /
-- user_password_credentials / user_auth_identities / user_sessions / user_roles
-- 20 users for search/pagination testing
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- auth.roles — V1 마이그레이션이 기본 3개를 insert하므로 여기서는 idempotent 보강만.
-- ------------------------------------------------------------
INSERT INTO auth.roles (id, code, display_name, description) VALUES
  (1, 'SUPER_ADMIN', '수퍼 관리자', '시스템 전체 권한'),
  (2, 'ADMIN',       '관리자',     '운영 관리 권한'),
  (3, 'MODERATOR',   '운영진',     '커뮤니티 모더레이션 권한')
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  display_name = EXCLUDED.display_name,
  description = EXCLUDED.description;

SELECT setval(pg_get_serial_sequence('auth.roles','id'),
              (SELECT MAX(id) FROM auth.roles));

-- ------------------------------------------------------------
-- auth.users  (20 users, lifecycle만)
-- UUID pattern: a0000000-0000-4000-8000-00000000000N
-- email / display_name / profile_image_url은 별도 테이블로 분리 (DBML 기준)
-- ------------------------------------------------------------
INSERT INTO auth.users (id, status, last_login_at) VALUES
  ('a0000000-0000-4000-8000-000000000001','ACTIVE',   '2026-06-12 09:20:00+09'),
  ('a0000000-0000-4000-8000-000000000002','ACTIVE',   '2026-06-13 14:05:00+09'),
  ('a0000000-0000-4000-8000-000000000003','ACTIVE',   '2026-06-14 08:30:00+09'),
  ('a0000000-0000-4000-8000-000000000004','ACTIVE',   '2026-06-11 20:15:00+09'),
  ('a0000000-0000-4000-8000-000000000005','ACTIVE',   '2026-06-14 10:00:00+09'),
  ('a0000000-0000-4000-8000-000000000006','ACTIVE',   '2026-06-10 12:00:00+09'),
  ('a0000000-0000-4000-8000-000000000007','ACTIVE',   '2026-06-13 18:45:00+09'),
  ('a0000000-0000-4000-8000-000000000008','ACTIVE',   '2026-06-09 11:30:00+09'),
  ('a0000000-0000-4000-8000-000000000009','ACTIVE',   '2026-06-08 16:20:00+09'),
  ('a0000000-0000-4000-8000-000000000010','ACTIVE',   '2026-06-14 07:10:00+09'),
  ('a0000000-0000-4000-8000-000000000011','ACTIVE',   '2026-06-07 13:50:00+09'),
  ('a0000000-0000-4000-8000-000000000012','ACTIVE',   '2026-06-06 09:40:00+09'),
  ('a0000000-0000-4000-8000-000000000013','ACTIVE',   '2026-06-05 19:25:00+09'),
  ('a0000000-0000-4000-8000-000000000014','ACTIVE',   '2026-06-04 15:35:00+09'),
  ('a0000000-0000-4000-8000-000000000015','ACTIVE',   '2026-06-03 10:15:00+09'),
  ('a0000000-0000-4000-8000-000000000016','SUSPENDED','2026-05-28 14:00:00+09'),
  ('a0000000-0000-4000-8000-000000000017','ACTIVE',   '2026-06-02 08:55:00+09'),
  ('a0000000-0000-4000-8000-000000000018','ACTIVE',   '2026-06-01 21:30:00+09'),
  ('a0000000-0000-4000-8000-000000000019','ACTIVE',   '2026-05-30 12:45:00+09'),
  ('a0000000-0000-4000-8000-000000000020','ACTIVE',   '2026-05-29 17:05:00+09')
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_email_addresses  (primary email per user)
-- ------------------------------------------------------------
INSERT INTO auth.user_email_addresses (user_id, email, normalized_email, is_primary, verified_at) VALUES
  ('a0000000-0000-4000-8000-000000000001','jihoon.kim@example.com',  'jihoon.kim@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000002','seoyeon.park@example.com','seoyeon.park@example.com',true, now()),
  ('a0000000-0000-4000-8000-000000000003','doyun.lee@example.com',   'doyun.lee@example.com',   true, now()),
  ('a0000000-0000-4000-8000-000000000004','minseo.choi@example.com', 'minseo.choi@example.com', true, now()),
  ('a0000000-0000-4000-8000-000000000005','yejun.jung@example.com',  'yejun.jung@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000006','hajun.kang@example.com',  'hajun.kang@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000007','seoa.yoon@example.com',   'seoa.yoon@example.com',   true, now()),
  ('a0000000-0000-4000-8000-000000000008','jia.cho@example.com',     'jia.cho@example.com',     true, now()),
  ('a0000000-0000-4000-8000-000000000009','siwoo.lim@example.com',   'siwoo.lim@example.com',   true, now()),
  ('a0000000-0000-4000-8000-000000000010','subin.bae@example.com',   'subin.bae@example.com',   true, now()),
  ('a0000000-0000-4000-8000-000000000011','jiho.oh@example.com',     'jiho.oh@example.com',     true, now()),
  ('a0000000-0000-4000-8000-000000000012','seojun.han@example.com',  'seojun.han@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000013','jiyu.shin@example.com',   'jiyu.shin@example.com',   true, now()),
  ('a0000000-0000-4000-8000-000000000014','dohyun.kwon@example.com', 'dohyun.kwon@example.com', true, now()),
  ('a0000000-0000-4000-8000-000000000015','harin.baek@example.com',  'harin.baek@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000016','minjae.song@example.com', 'minjae.song@example.com', true, now()),
  ('a0000000-0000-4000-8000-000000000017','yewon.jeon@example.com',  'yewon.jeon@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000018','junseo.hwang@example.com','junseo.hwang@example.com',true, now()),
  ('a0000000-0000-4000-8000-000000000019','hayoon.ryu@example.com',  'hayoon.ryu@example.com',  true, now()),
  ('a0000000-0000-4000-8000-000000000020','seoyun.ko@example.com',   'seoyun.ko@example.com',   true, now())
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_profiles  (display data per user)
-- ------------------------------------------------------------
INSERT INTO auth.user_profiles (user_id, display_name, profile_image_url, bio, profile_visibility) VALUES
  ('a0000000-0000-4000-8000-000000000001','김지훈','https://cdn.soomgil.test/avatar/u01.png','여행의 즐거움을 찾아 떠나는 사람.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000002','박서연','https://cdn.soomgil.test/avatar/u02.png','자연과 산책을 사랑하는 힐링 여행러.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000003','이도윤','https://cdn.soomgil.test/avatar/u03.png','온천과 휴양을 좋아합니다.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000004','최민서','https://cdn.soomgil.test/avatar/u04.png','자전거 여행 러버.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000005','정예준','https://cdn.soomgil.test/avatar/u05.png','야경 사진 전문.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000006','강하준','https://cdn.soomgil.test/avatar/u06.png','드라이브 코스 전문가.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000007','윤서아','https://cdn.soomgil.test/avatar/u07.png','숲길 산책 마니아.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000008','조지아','https://cdn.soomgil.test/avatar/u08.png','커피와 디저트 사랑.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000009','임시우','https://cdn.soomgil.test/avatar/u09.png','골목길 출사 여행러.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000010','배수빈','https://cdn.soomgil.test/avatar/u10.png','빈티지 감성 카페 러버.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000011','오지호','https://cdn.soomgil.test/avatar/u11.png','전통시장 미식가.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000012','한서준','https://cdn.soomgil.test/avatar/u12.png','가족 여행 슈퍼대디.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000013','신지유','https://cdn.soomgil.test/avatar/u13.png','카페 투어 전문가.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000014','권도현','https://cdn.soomgil.test/avatar/u14.png','피톤치드 러버.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000015','백하린','https://cdn.soomgil.test/avatar/u15.png','로드트립 매니아.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000016','송민재','https://cdn.soomgil.test/avatar/u16.png','정지된 계정.','PRIVATE'),
  ('a0000000-0000-4000-8000-000000000017','전예원','https://cdn.soomgil.test/avatar/u17.png','사진 찍는 여행러.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000018','황준서','https://cdn.soomgil.test/avatar/u18.png','드라이브 코스 좋아합니다.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000019','류하윤','https://cdn.soomgil.test/avatar/u19.png','힐링 여행러.','PUBLIC'),
  ('a0000000-0000-4000-8000-000000000020','고서윤','https://cdn.soomgil.test/avatar/u20.png','여행의 즐거움을 찾아 떠나는 사람.','PUBLIC')
ON CONFLICT (user_id) DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_settings  (defaults — ON CONFLICT로 이미 있으면 skip)
-- ------------------------------------------------------------
INSERT INTO auth.user_settings (user_id)
SELECT id FROM auth.users
ON CONFLICT (user_id) DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_password_credentials (LOCAL users 1~5)
-- ------------------------------------------------------------
INSERT INTO auth.user_password_credentials (user_id, password_hash, password_algorithm) VALUES
  ('a0000000-0000-4000-8000-000000000001','$2a$10$DUMMYHASHjihoonkim000000000000000000000000000000','bcrypt'),
  ('a0000000-0000-4000-8000-000000000002','$2a$10$DUMMYHASHseoyeonpark00000000000000000000000000000','bcrypt'),
  ('a0000000-0000-4000-8000-000000000003','$2a$10$DUMMYHASHdoyunlee00000000000000000000000000000000','bcrypt'),
  ('a0000000-0000-4000-8000-000000000004','$2a$10$DUMMYHASHminseochoi0000000000000000000000000000000','bcrypt'),
  ('a0000000-0000-4000-8000-000000000005','$2a$10$DUMMYHASHyejunjung00000000000000000000000000000000','bcrypt')
ON CONFLICT (user_id) DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_auth_identities (KAKAO / GOOGLE)
-- DBML 컬럼: provider_profile (raw_profile 아님)
-- ------------------------------------------------------------
INSERT INTO auth.user_auth_identities (id, user_id, provider_id, provider_subject, provider_email, provider_profile) VALUES
  ('d1000000-0000-4000-8000-000000000006','a0000000-0000-4000-8000-000000000006',2,'kakao:2900000006','hajun.kakao@example.com',  '{"nick":"하준카페"}'),
  ('d1000000-0000-4000-8000-000000000007','a0000000-0000-4000-8000-000000000007',3,'google:sub1000007','seoa.google@example.com', '{"locale":"ko"}'),
  ('d1000000-0000-4000-8000-000000000008','a0000000-0000-4000-8000-000000000008',2,'kakao:2900000008','jia.kakao@example.com',    '{"nick":"지아트래블"}'),
  ('d1000000-0000-4000-8000-000000000009','a0000000-0000-4000-8000-000000000009',3,'google:sub1000009','siwoo.google@example.com', '{}'),
  ('d1000000-0000-4000-8000-000000000010','a0000000-0000-4000-8000-000000000010',2,'kakao:2900000010','subin.kakao@example.com',  '{"nick":"수빈여행"}'),
  ('d1000000-0000-4000-8000-000000000011','a0000000-0000-4000-8000-000000000011',3,'google:sub1000011','jiho.google@example.com',  '{}'),
  ('d1000000-0000-4000-8000-000000000012','a0000000-0000-4000-8000-000000000012',2,'kakao:2900000012','seojun.kakao@example.com', '{}'),
  ('d1000000-0000-4000-8000-000000000013','a0000000-0000-4000-8000-000000000013',3,'google:sub1000013','jiyu.google@example.com',  '{}'),
  ('d1000000-0000-4000-8000-000000000014','a0000000-0000-4000-8000-000000000014',2,'kakao:2900000014','dohyun.kakao@example.com', '{}'),
  ('d1000000-0000-4000-8000-000000000015','a0000000-0000-4000-8000-000000000015',3,'google:sub1000015','harin.google@example.com', '{}')
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_sessions
-- DBML 컬럼: user_agent_hash, ip_address_hash (user_agent/ip_address 아님)
-- ------------------------------------------------------------
INSERT INTO auth.user_sessions (id, user_id, refresh_token_hash, user_agent_hash, ip_address_hash, expires_at) VALUES
  ('c1000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-000000000001','hash_token_jihoon_001',  'hash_ua_windows','hash_ip_203_0_113_1', '2026-07-12 09:20:00+09'),
  ('c1000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000002','hash_token_seoyeon_002', 'hash_ua_macos',  'hash_ip_203_0_113_2', '2026-07-13 14:05:00+09'),
  ('c1000000-0000-4000-8000-000000000003','a0000000-0000-4000-8000-000000000003','hash_token_doyun_003',   'hash_ua_linux',  'hash_ip_203_0_113_3', '2026-07-14 08:30:00+09'),
  ('c1000000-0000-4000-8000-000000000004','a0000000-0000-4000-8000-000000000004','hash_token_minseo_004',  'hash_ua_android','hash_ip_203_0_113_4', '2026-07-11 20:15:00+09'),
  ('c1000000-0000-4000-8000-000000000005','a0000000-0000-4000-8000-000000000005','hash_token_yejun_005',   'hash_ua_ios',    'hash_ip_203_0_113_5', '2026-07-14 10:00:00+09'),
  ('c1000000-0000-4000-8000-000000000006','a0000000-0000-4000-8000-000000000006','hash_token_hajun_006',   'hash_ua_ios',    'hash_ip_203_0_113_6', '2026-07-10 12:00:00+09'),
  ('c1000000-0000-4000-8000-000000000007','a0000000-0000-4000-8000-000000000007','hash_token_seoa_007',    'hash_ua_android','hash_ip_203_0_113_7', '2026-07-13 18:45:00+09'),
  ('c1000000-0000-4000-8000-000000000008','a0000000-0000-4000-8000-000000000010','hash_token_subin_010',   'hash_ua_macos',  'hash_ip_203_0_113_10','2026-07-14 07:10:00+09')
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- auth.user_roles (DBML 기준: user_id + role_id + granted_by_user_id)
-- ------------------------------------------------------------
INSERT INTO auth.user_roles (user_id, role_id, granted_by_user_id) VALUES
  ('a0000000-0000-4000-8000-000000000001',1,'a0000000-0000-4000-8000-000000000001'),
  ('a0000000-0000-4000-8000-000000000002',2,'a0000000-0000-4000-8000-000000000001'),
  ('a0000000-0000-4000-8000-000000000003',2,'a0000000-0000-4000-8000-000000000001'),
  ('a0000000-0000-4000-8000-000000000016',3,'a0000000-0000-4000-8000-000000000001')
ON CONFLICT (user_id, role_id) DO NOTHING;

COMMIT;
