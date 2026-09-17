# 관광공사 데이터 축적과 재사용

스케줄러 없이 사용자 요청으로 필요한 데이터만 수집합니다. Flyway V52~V53 적용 후 사용합니다.

## 조회 정책

- 검색·지도·취향 목록은 `tourism_source.attractions`를 먼저 조회합니다. 일치하는 DB 데이터가 있으면 외부 목록 API를 호출하지 않습니다.
- DB에 결과가 없으면 외부 목록의 한 페이지만 가져옵니다. 목록 전체에 대한 상세·사진·이용 정보 선조회는 하지 않습니다.
- 상세 정보는 필요할 때 수집하며 성공 응답(빈 결과 포함)을 `tourism_source.kto_responses`에 영속 저장합니다. 인증키는 저장하지 않습니다.
- 같은 요청의 동시 수집은 PostgreSQL advisory lock으로 합칩니다. 실패는 5분부터 최대 60분까지 재시도를 제한하고 기존 성공 데이터를 유지합니다. 할당량 초과는 해당 서비스에 1시간 호출 제한을 적용합니다.
- 재시도 제한이 끝나도 자동 호출하지 않습니다. 다음 사용자 요청에서만 재시도합니다.
- `GET /api/v1/places/{provider}/{id}?includeInfo=false`는 이용·접근성 정보 조회를 생략합니다. 사진 모달에서는 정보 펼치기 전까지 이 옵션을 사용합니다.
- 자동 만료·주기적 최신화는 없습니다. DB 결과만 반환하므로 관광공사의 전체 최신 목록과 일치하지 않을 수 있습니다. 기존 Redis 조각 캐시를 일괄 이관하지는 않습니다.

## 관광 데이터 seed

아래 명령은 프로젝트 루트에서 실행합니다. Docker PostgreSQL 컨테이너가 실행 중이어야 합니다.

```powershell
python backend/tools/tourism_seed.py export backend/seeds/local/tourism.json
python backend/tools/tourism_seed.py import backend/seeds/local/tourism.json --dry-run
python backend/tools/tourism_seed.py import backend/seeds/local/tourism.json
```

`--dry-run`은 파일 구조와 SQL 생성만 검증하며 DB 제약을 실행 검증하지 않습니다. 실제 import는 단일 트랜잭션으로 처리합니다.

사용자·여행·취향 데이터는 제외하고 관광지, 공개 이미지 URL, 콘텐츠 유형, 성공한 원천 응답만 내보냅니다. `seeds/local/`은 Git에서 제외됩니다. 내보내기와 가져오기는 관광공사 API를 호출하지 않습니다.

관광지 `content_id`, 사진 `content_id + public_url`, 응답 `request_key`로 중복을 처리합니다. 원천 수정 시각이 더 오래된 데이터와 비어 있는 필드는 기존 정상 정보를 덮어쓰지 않습니다. 동일 seed를 반복 적용해도 장소·사진이 늘어나지 않습니다.

## 특정 장소 수동 갱신

```powershell
python backend/tools/tourism_seed.py refresh 126508
```

즉시 API를 호출하지 않고 다음 상세 조회에 갱신하도록 표시합니다. 실패하면 기존 정보를 반환하며 재시도 제한도 유지합니다. 새 DB에는 먼저 seed를 가져오면 외부 호출 없이 축적된 데이터로 시작할 수 있습니다.

## 관측

Micrometer 카운터 `soomgil.kto.requests`, `soomgil.kto.cache.hit`, `soomgil.kto.failures`를 endpoint 태그로 기록합니다. requests는 실제 외부 요청 시도 수입니다.
