# 대시보드 데모 데이터

`soomgil_demo_seoul_daejeon.sql`은 Flyway V1~V38 적용 후 넣는 로컬 전용 데이터입니다.
운영 마이그레이션에는 포함되지 않으며 기존 데이터를 삭제하지 않습니다.

`load-seeds.sh`는 서울 기본 시드와 제주·부산·경주·커뮤니티 시드를 합쳐
`generated/soomgil_demo_dashboard_dump.sql`을 다시 만든 후 적용합니다. 마지막에는
실제 KTO API 응답 스냅샷과 `soomgil_jeju_place_tags.sql`의 제주 AI 태그를 적용한 뒤
`verify_demo_data.sql`로 품질 조건을 검사합니다.

포함 범위:

- 공개 프로필 사용자 120명과 촘촘한 팔로우 관계
- 서울 40곳, 대전 28곳의 검색 가능한 실재 장소와 이미지·취향 태그
- 스와이프 반응, 저장 장소, 사용자별 취향 가중치
- 원본·파생 여행 80여 개와 일차별 일정, 경로, 메모, 체크리스트, 채팅
- 여행·커뮤니티·프로필에 연결된 실제 S3 호환 미디어
- 테스트 계정의 진행 중 여행 3개·보관 여행 1개, 실제 KTO 장소와 작성 글 2개
- 대표 관광지가 겹치지 않는 커뮤니티 게시물 18개
- 선정 게시물의 기존 좋아요 263개
- 선정 게시물의 기존 댓글·대댓글 37개
- 게시물 스냅샷에서 복제된 리트립 여행 24개
- 알림과 운영 감사 로그
- 전국 17개 지역 KTO 장소 50,915곳의 Gemini 2.5 Flash Lite 태그 결과 (V40 + V55)

## 적용

백엔드를 한 번 실행해 Flyway 마이그레이션을 적용하고 PostgreSQL 컨테이너가 실행 중인 상태에서:

```bash
bash load-seeds.sh
```

생성된 dump만 직접 적용할 수도 있습니다.

```bash
docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil \
  -v ON_ERROR_STOP=1 < seeds/generated/soomgil_demo_dashboard_dump.sql
docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil \
  -v ON_ERROR_STOP=1 < seeds/soomgil_jeju_place_tags.sql
```

## AI 장소 태그

전국 KTO 장소 50,915곳의 태그는 Flyway 마이그레이션으로 들어갑니다. 백엔드를 한 번
띄우면 자동으로 적재되므로 따로 실행할 것은 없습니다.

- `V40__seed_jeju_place_tag_enrichments.sql` — 제주(39) 2,335곳
- `V55__seed_nationwide_place_tag_enrichments.sql` — 나머지 16개 지역 48,580곳

`seeds/soomgil_jeju_place_tags.sql`은 V40과 같은 내용이며, 데모 dump를 적용한 뒤
제주 태그를 다시 넣기 위해 `load-seeds.sh`와 `init-demo-dump.mjs`가 사용합니다.

셋 다 모든 행을 임시 테이블에 먼저 넣은 뒤 병합합니다. 같은 장소의 enrichment가
이미 있으면 그 행의 id를 그대로 재사용하므로 `user_place_reactions` /
`user_swipe_events` / `synthetic_swipe_events` 가 참조 중이어도 FK 위반 없이
갱신되고, 여러 번 실행해도 결과가 같습니다.

> V40과 시드 파일은 기존에 enrichment를 지우고 다시 넣는 방식이라, 데모 데이터가
> 제주 enrichment를 참조하고 있으면 `fk_user_place_reactions_enrichment` 위반으로
> 실패했습니다. 이번에 위 병합 방식으로 교체했습니다. 이미 V40을 적용한 DB는
> checksum이 달라져 백엔드가 뜨지 않으므로, 데모 DB를 새로 만들거나
> (`node init-demo-dump.mjs`) checksum만 비우면 됩니다(데이터는 동일):
>
> ```bash
> docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil \
>   -c "UPDATE flyway_schema_history SET checksum = NULL WHERE version = '40';"
> ```

## S3 이미지 동기화

시드를 넣은 다음 `.env`의 AWS S3 설정을 사용해 프로필, 장소, 여행,
커뮤니티 이미지를 비공개 S3에 업로드합니다.

로컬에서는 별도 설정이 없으면 `S3_BUCKET=soomgil-local`,
`S3_PUBLIC_BASE_URL=http://localhost:9000/soomgil-local`을 사용합니다. 이후 AWS 배포에서는
같은 시드나 코드를 수정하지 않고 두 환경변수만 실제 S3 버킷과 CloudFront 주소로
교체합니다. CloudFront 배포 도메인은 시드 파일에 하드코딩하지 않습니다.

```bash
python3 -m venv /tmp/soomgil-demo-media-venv
/tmp/soomgil-demo-media-venv/bin/pip install -r seeds/requirements.txt
/tmp/soomgil-demo-media-venv/bin/python seeds/sync_demo_media.py --new-only
```

동기화기는 DB에서 필요한 객체 목록을 읽고 현재 데모 기준 340개 파일을 업로드한 뒤 각 객체를
`HeadObject`로 다시 확인합니다. 출처, 라이선스, 체크섬은
`seeds/generated/demo-media-manifest.csv`에 기록됩니다.

PowerShell에서 직접 적용할 수도 있습니다.

```powershell
Get-Content -Raw .\seeds\soomgil_demo_seoul_daejeon.sql |
  docker compose exec -T postgres psql -U soomgil -d soomgil -v ON_ERROR_STOP=1
```

스크립트 마지막에는 주요 데이터별 적재 건수가 출력됩니다. 같은 파일을 다시 실행해도 고정 식별자와 충돌 처리 덕분에 데이터가 중복되지 않습니다.
