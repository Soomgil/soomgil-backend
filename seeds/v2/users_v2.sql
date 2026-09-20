-- ============================================================================
-- Soomgil demo dataset v2 — users
-- 데모 제출용 2계정 + 협업 멤버/커뮤니티 작성자 12명. 비밀번호는 전원 Soomgil123!
-- 데모 2계정은 온보딩을 API로 실제 완료시켜(가입 취향 설문 10곳) 스와이프 기록을 남기므로
-- onboarding_completed_at 을 비워 둔다. 나머지는 바로 앱을 쓸 수 있게 완료 처리한다.
-- ============================================================================
\set ON_ERROR_STOP on
BEGIN;

WITH seed(n, email, display_name, bio, is_demo) AS (VALUES
  ( 1,'demo1@soomgil.app','김민아','여행 계획 세우는 걸 여행만큼 좋아하는 플래너. 걷기 좋은 골목과 로컬 빵집을 찾아다녀요.', true),
  ( 2,'demo2@soomgil.app','박준호','바다랑 캠핑이면 어디든. 사진 찍고 지도에 그림 그리며 일정 짜는 게 취미입니다.', true),
  ( 3,'seoyeon@soomgil.app','이서연','미술관과 독립서점, 조용한 카페가 있는 도시 여행을 좋아해요.', false),
  ( 4,'doyun@soomgil.app','최도윤','맛집은 줄 서서라도 먹는 편. 시장 투어 담당.', false),
  ( 5,'hana@soomgil.app','정하나','일출·일몰 스팟 수집가. 새벽에 잘 일어납니다.', false),
  ( 6,'jiwoo@soomgil.app','한지우','트레킹과 오름, 숲길. 걷다가 만나는 풍경이 제일 좋아요.', false),
  ( 7,'yerin@soomgil.app','오예린','아이 둘과 다니는 가족 여행러. 유모차 동선 정보 나눠요.', false),
  ( 8,'taeyang@soomgil.app','강태양','자전거 타고 강변 따라 달리는 여행. 레일바이크도 좋아합니다.', false),
  ( 9,'sua@soomgil.app','윤수아','한옥 스테이와 야경. 밤 산책 코스 위주로 다닙니다.', false),
  (10,'minseok@soomgil.app','장민석','커피 한 잔 들고 해변 걷기. 강릉·부산 바다 전문.', false),
  (11,'eunji@soomgil.app','송은지','역사 유적지 좋아하는 문화재 덕후. 해설 들으며 천천히.', false),
  (12,'woojin@soomgil.app','임우진','혼행러. 교통 편한 코스와 조용한 숙소를 정리해 둡니다.', false),
  (13,'chaewon@soomgil.app','배채원','디저트 지도 만드는 사람. 성심당은 세 번 갔어요.', false),
  (14,'hyunwoo@soomgil.app','서현우','낚시와 항구 풍경. 주문진·미포항 단골.', false)
)
INSERT INTO auth.users (id, status, last_login_at, onboarding_completed_at, created_at, updated_at)
SELECT md5('demo-v2-user:' || n)::uuid, 'ACTIVE',
       now() - make_interval(hours => n * 3),
       CASE WHEN is_demo THEN NULL ELSE now() - make_interval(days => 120 - n) END,
       now() - make_interval(days => 140 - n * 2), now()
FROM seed
ON CONFLICT (id) DO NOTHING;

WITH seed(n, email, display_name, bio) AS (VALUES
  ( 1,'demo1@soomgil.app','김민아','여행 계획 세우는 걸 여행만큼 좋아하는 플래너. 걷기 좋은 골목과 로컬 빵집을 찾아다녀요.'),
  ( 2,'demo2@soomgil.app','박준호','바다랑 캠핑이면 어디든. 사진 찍고 지도에 그림 그리며 일정 짜는 게 취미입니다.'),
  ( 3,'seoyeon@soomgil.app','이서연','미술관과 독립서점, 조용한 카페가 있는 도시 여행을 좋아해요.'),
  ( 4,'doyun@soomgil.app','최도윤','맛집은 줄 서서라도 먹는 편. 시장 투어 담당.'),
  ( 5,'hana@soomgil.app','정하나','일출·일몰 스팟 수집가. 새벽에 잘 일어납니다.'),
  ( 6,'jiwoo@soomgil.app','한지우','트레킹과 오름, 숲길. 걷다가 만나는 풍경이 제일 좋아요.'),
  ( 7,'yerin@soomgil.app','오예린','아이 둘과 다니는 가족 여행러. 유모차 동선 정보 나눠요.'),
  ( 8,'taeyang@soomgil.app','강태양','자전거 타고 강변 따라 달리는 여행. 레일바이크도 좋아합니다.'),
  ( 9,'sua@soomgil.app','윤수아','한옥 스테이와 야경. 밤 산책 코스 위주로 다닙니다.'),
  (10,'minseok@soomgil.app','장민석','커피 한 잔 들고 해변 걷기. 강릉·부산 바다 전문.'),
  (11,'eunji@soomgil.app','송은지','역사 유적지 좋아하는 문화재 덕후. 해설 들으며 천천히.'),
  (12,'woojin@soomgil.app','임우진','혼행러. 교통 편한 코스와 조용한 숙소를 정리해 둡니다.'),
  (13,'chaewon@soomgil.app','배채원','디저트 지도 만드는 사람. 성심당은 세 번 갔어요.'),
  (14,'hyunwoo@soomgil.app','서현우','낚시와 항구 풍경. 주문진·미포항 단골.')
)
INSERT INTO auth.user_profiles (user_id, display_name, profile_image_url, bio, profile_visibility, created_at, updated_at)
SELECT md5('demo-v2-user:' || n)::uuid, display_name, NULL, bio, 'PUBLIC',
       now() - make_interval(days => 140 - n * 2), now()
FROM seed
ON CONFLICT (user_id) DO UPDATE SET display_name = EXCLUDED.display_name, bio = EXCLUDED.bio, updated_at = now();

WITH seed(n, email) AS (VALUES
  (1,'demo1@soomgil.app'),(2,'demo2@soomgil.app'),(3,'seoyeon@soomgil.app'),(4,'doyun@soomgil.app'),
  (5,'hana@soomgil.app'),(6,'jiwoo@soomgil.app'),(7,'yerin@soomgil.app'),(8,'taeyang@soomgil.app'),
  (9,'sua@soomgil.app'),(10,'minseok@soomgil.app'),(11,'eunji@soomgil.app'),(12,'woojin@soomgil.app'),
  (13,'chaewon@soomgil.app'),(14,'hyunwoo@soomgil.app')
)
INSERT INTO auth.user_email_addresses (id, user_id, email, normalized_email, is_primary, verified_at, created_at, updated_at)
SELECT md5('demo-v2-email:' || n)::uuid, md5('demo-v2-user:' || n)::uuid, email, lower(email), true,
       now() - make_interval(days => 140 - n * 2), now() - make_interval(days => 140 - n * 2), now()
FROM seed
ON CONFLICT DO NOTHING;

INSERT INTO auth.user_settings (user_id, display_language, timezone, marketing_email_opt_in, trip_invite_email_opt_in, created_at, updated_at)
SELECT md5('demo-v2-user:' || n)::uuid, 'ko', 'Asia/Seoul', false, true, now() - make_interval(days => 140 - n * 2), now()
FROM generate_series(1, 14) n
ON CONFLICT (user_id) DO NOTHING;

-- 공용 로컬/데모 비밀번호: Soomgil123!
INSERT INTO auth.user_password_credentials (user_id, password_hash, password_algorithm, password_changed_at, created_at, updated_at)
SELECT md5('demo-v2-user:' || n)::uuid,
       '$2a$10$4zIe8rBz3.2nGyj4JC/8uug1K82T9xDq74iVLGlgN1b5hzz5YEpCe', 'bcrypt',
       now() - interval '60 days', now() - interval '60 days', now()
FROM generate_series(1, 14) n
ON CONFLICT (user_id) DO UPDATE SET password_hash = EXCLUDED.password_hash, password_algorithm = EXCLUDED.password_algorithm, updated_at = now();

INSERT INTO auth.user_policy_acceptances (id, user_id, policy_document_id, acceptance_method, accepted_at, created_at)
SELECT md5('demo-v2-policy:' || n || ':' || p.id)::uuid, md5('demo-v2-user:' || n)::uuid, p.id, 'EXPLICIT',
       now() - make_interval(days => 140 - n * 2), now() - make_interval(days => 140 - n * 2)
FROM generate_series(1, 14) n
CROSS JOIN auth.policy_documents p
WHERE p.retired_at IS NULL AND p.is_required
ON CONFLICT DO NOTHING;

COMMIT;
