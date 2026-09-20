# 데모 데이터 v2

제출·시연용 데이터 세트입니다. **실제 백엔드 API로 생성**하므로 FK·일정 버전·알림·감사 기록이 진짜 사용 흔적처럼 남고,
모든 사진은 DB에 이미 들어 있는 **한국관광공사(KTO) 실제 관광지 이미지**를 내려받아 미디어 API로 업로드한 것입니다.
(예전 Unsplash/pravatar 기반 시드는 제거했습니다.)

## 계정

| 구분 | 이메일 | 비밀번호 | 설명 |
| --- | --- | --- | --- |
| **제출용 데모 1** | `demo1@soomgil.app` | `Soomgil123!` | 김민아 — 서울·부산·경주 여행방 방장, 나머지 방 멤버 |
| **제출용 데모 2** | `demo2@soomgil.app` | `Soomgil123!` | 박준호 — 대전·제주·강릉 여행방 방장, 나머지 방 멤버 |
| 협업 멤버 / 커뮤니티 작성자 12명 | `seoyeon@`, `doyun@`, `hana@`, `jiwoo@`, `yerin@`, `taeyang@`, `sua@`, `minseok@`, `eunji@`, `woojin@`, `chaewon@`, `hyunwoo@soomgil.app` | `Soomgil123!` | 여행방 참여, 채팅·체크리스트, 커뮤니티 글 작성 |

두 데모 계정은 가입 취향 설문(10곳)을 API로 실제 완료했고, 지역 스와이프 피드로 20곳을 더 반응해 **각 30개 스와이프**가 쌓여 있습니다.

## 들어 있는 것

- 여행방 6개(서울 종로·중구 / 대전 / 부산 해운대 / 제주 / 경주 / 강릉) — 두 데모 계정이 모두 멤버. 각 방마다
  일차별 일정(실제 KTO 장소·좌표·사진), 장소 사이 경로(Mapbox 맵매칭), 여행 메모 + 1일차 메모,
  준비물 체크리스트 + 1일차 체크리스트(멤버마다 다른 완료 상태), 멤버 채팅, 미정 장소.
- 서울 방에는 **완료된 투표** 1건(방장 개설 → 멤버 전원 스티커 제출 → 종료 → 선정 장소가 일정에 반영).
- 데모 방 2곳에 실제 AI 가이드 대화.
- 커뮤니티 글 18편(지역·장소에 맞는 제목/본문, 해시태그, 글마다 KTO 실사진 2~4장), 좋아요·댓글·답글, 리트립 2건, 팔로우 관계.
- 프로필 사진(전원), 저장한 장소, 알림(초대·팔로우).

## 적용 (팀원 / 서버)

빈 DB에 Flyway 마이그레이션(전국 법정동·관광지·AI 태그)을 올린 뒤 덤프를 넣습니다. 로컬은 한 줄:

```bash
node init-demo-dump.mjs
```

이미지는 로컬 MinIO(`soomgil-local` 버킷)에 있어야 합니다. 로컬은 생성 시점에 이미 올라가 있고,
**다른 머신/AWS**에서는 매니페스트로 같은 object_key 에 다시 올립니다(`.env.aws`의 S3 값 사용):

```bash
python3 backend/seeds/v2/sync_media_v2.py --env .env.aws
```

## 다시 만들기 (데이터 세트를 바꾸고 싶을 때)

```bash
node backend/seeds/v2/reset_schema_only.mjs                      # DB 비우고 Flyway 스키마만
docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil -v ON_ERROR_STOP=1 < backend/seeds/v2/users_v2.sql
node backend/seeds/v2/generate_demo_v2.mjs                       # API로 전부 생성 + KTO 사진 업로드 + 타임라인 흩뿌리기
docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil -v ON_ERROR_STOP=1 < backend/seeds/v2/verify_demo_v2.sql
bash backend/seeds/v2/build_dump_v2.sh                           # generated/soomgil_demo_dashboard_dump.sql 갱신
```

여행방·글 내용은 `generate_demo_v2.mjs` 상단의 `TRIPS` / `POSTS` 상수에서 고칩니다(장소는 KTO content_id).
`verify_demo_v2.sql`은 사진 없는 글, 빈 여행방, 데모 계정 스와이프 부족, 옛 스톡 이미지 URL 잔존 등을 잡아 실패시킵니다.

## AI 장소 태그

전국 KTO 장소 50,915곳의 태그는 Flyway(V40/V55)가 넣습니다. 덤프는 이를 건드리지 않으므로 별도 단계가 없습니다.
