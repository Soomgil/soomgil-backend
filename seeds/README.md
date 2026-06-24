# 대시보드 데모 데이터

`soomgil_demo_seoul_daejeon.sql`은 Flyway V1~V38 적용 후 넣는 로컬 전용 데이터입니다.
운영 마이그레이션에는 포함되지 않으며 기존 데이터를 삭제하지 않습니다.

`load-seeds.sh`는 서울 기본 시드와 제주·부산·경주·여행 기록 시드를 합쳐
`generated/soomgil_demo_dashboard_dump.sql`을 다시 만든 후 적용합니다. 마지막에는
실제 KTO API 응답 스냅샷을 덮어쓰고 `verify_demo_data.sql`로 품질 조건을 검사합니다.

포함 범위:

- 공개 프로필 사용자 120명과 촘촘한 팔로우 관계
- 서울 40곳, 대전 28곳의 검색 가능한 실재 장소와 이미지·취향 태그
- 스와이프 반응, 저장 장소, 사용자별 취향 가중치
- 원본·파생 여행 80여 개와 일차별 일정, 경로, 메모, 체크리스트, 채팅
- 완료 여행 기록 225개와 실제 S3 미디어, 테스트 계정용 세로 기록 사진 5장
- 테스트 계정의 진행 중 여행 3개·보관 여행 1개, 실제 KTO 장소와 작성 글 2개
- 서로 다른 제목·요약의 커뮤니티 게시물 59개
- 게시물마다 다른 좋아요 수 3,297개
- 내용이 모두 다른 댓글·대댓글 320개
- 게시물 스냅샷에서 복제된 리트립 여행 24개
- 알림과 운영 감사 로그

## 적용

백엔드를 한 번 실행해 Flyway 마이그레이션을 적용하고 PostgreSQL 컨테이너가 실행 중인 상태에서:

```bash
bash load-seeds.sh
```

생성된 dump만 직접 적용할 수도 있습니다.

```bash
docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil \
  -v ON_ERROR_STOP=1 < seeds/generated/soomgil_demo_dashboard_dump.sql
```

## S3 이미지 동기화

시드를 넣은 다음 `.env`의 AWS S3 설정을 사용해 프로필, 장소, 여행 기록,
커뮤니티 이미지를 비공개 S3에 업로드합니다.

```bash
python3 -m venv /tmp/soomgil-demo-media-venv
/tmp/soomgil-demo-media-venv/bin/pip install -r seeds/requirements.txt
/tmp/soomgil-demo-media-venv/bin/python seeds/sync_demo_media.py --new-only
```

동기화기는 DB에서 필요한 객체 목록을 읽고 554개 파일을 업로드한 뒤 각 객체를
`HeadObject`로 다시 확인합니다. 출처, 라이선스, 체크섬은
`seeds/generated/demo-media-manifest.csv`에 기록됩니다.

PowerShell에서 직접 적용할 수도 있습니다.

```powershell
Get-Content -Raw .\seeds\soomgil_demo_seoul_daejeon.sql |
  docker compose exec -T postgres psql -U soomgil -d soomgil -v ON_ERROR_STOP=1
```

스크립트 마지막에는 주요 데이터별 적재 건수가 출력됩니다. 같은 파일을 다시 실행해도 고정 식별자와 충돌 처리 덕분에 데이터가 중복되지 않습니다.
