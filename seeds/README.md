# 서울·대전 데모 데이터

`soomgil_demo_seoul_daejeon.sql`은 Flyway V1~V36 적용 후 넣는 로컬 전용 데이터입니다.
운영 마이그레이션에는 포함되지 않으며 기존 데이터를 삭제하지 않습니다.

포함 범위:

- 공개 프로필 사용자 120명과 촘촘한 팔로우 관계
- 서울 40곳, 대전 28곳의 검색 가능한 실재 장소와 이미지·취향 태그
- 스와이프 반응, 저장 장소, 사용자별 취향 가중치
- 원본·파생 여행 80여 개와 일차별 일정, 경로, 메모, 체크리스트, 채팅
- 완료 여행 기록과 미디어 200여 개
- 커뮤니티 게시물 59개, 해시태그, 미디어, 좋아요 수천 개, 댓글 400여 개
- 게시물 스냅샷에서 복제된 리트립 여행 24개
- 알림과 운영 감사 로그

## 적용

백엔드를 한 번 실행해 Flyway 마이그레이션을 적용하고 PostgreSQL 컨테이너가 실행 중인 상태에서:

```bash
./load-seeds.sh
```

PowerShell에서 직접 적용할 수도 있습니다.

```powershell
Get-Content -Raw .\seeds\soomgil_demo_seoul_daejeon.sql |
  docker compose exec -T postgres psql -U soomgil -d soomgil -v ON_ERROR_STOP=1
```

스크립트 마지막에는 주요 데이터별 적재 건수가 출력됩니다. 같은 파일을 다시 실행해도 고정 식별자와 충돌 처리 덕분에 데이터가 중복되지 않습니다.
