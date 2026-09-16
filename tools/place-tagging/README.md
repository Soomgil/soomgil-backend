# 전국 장소 텍스트 태깅 (3명 분담)

SSAFY `SSAFY_TRIP_Dump.sql`의 **제주를 뺀 전국 장소**를 GMS(`gpt-5.5`, 제주 태깅과 같은 모델·프롬프트)로 태깅해
Soomgil PostgreSQL에 넣을 SQL을 만든다. 제주(`area_code=39`)는 `../jeju-tagging`으로 이미 끝났으므로 제외한다.

## 누가 뭘 하나

지역(시도) 단위로 장소 수가 비슷하게 3파트로 나눈다. 같은 덤프면 누가 돌려도 같은 결과로 나뉜다.

| 담당 | 실행 파일 |
| --- | --- |
| 1번 사람 | `run-A.bat` (mac: `run-A.command`) |
| 2번 사람 | `run-B.bat` (mac: `run-B.command`) |
| 3번 사람 | `run-C.bat` (mac: `run-C.command`) |

실행하면 맨 위에 A/B/C가 어느 지역 몇 곳인지 표로 나오고, 내 파트에 `◀ 이 컴퓨터`가 붙는다.
정확한 분배만 보고 싶으면 `python place_tagging.py plan`.

## 준비 (한 번만)

1. Python 3.10+ 설치.
2. `SSAFY_TRIP_Dump.sql`을 `내 PC\Downloads\SSAFY_HOME_TRIP_202604\`에 둔다.
   다른 곳에 있으면 그 파일을 `run-X.bat` 위로 드래그해서 실행하면 된다.
3. 프로젝트 루트 `.env`에 GMS 키를 넣는다. 키가 2개면 두 줄 다 넣는다.
   ```
   GMS_API_KEY=첫번째키
   GMS_API_KEY_2=두번째키
   GMS_CHAT_MODEL=gpt-5.5
   ```
   A와 C는 첫 키, B는 두 번째 키를 먼저 쓰고, 그 키가 막히면 자동으로 다른 키로 넘어간다.

## 실행

내 파트 파일을 **더블클릭**. 그게 전부다.

- 진행률, 이번 실행 요청 수, 키별 오늘 요청 수, 남은 예상 시간을 한 줄씩 찍는다.
- 결과는 10곳마다 `output/part-X/tagged.jsonl`에 바로 저장된다. **중간에 꺼져도 다시 더블클릭하면 이어서 한다.**
- SQL은 20요청마다, 그리고 끝날 때 `output/part-X/soomgil_place_tags_X.sql`로 갱신된다.
- 창을 닫고 싶으면 `Ctrl+C` 또는 그냥 닫기. 저장된 데까지의 SQL이 남는다.

## GMS 하루 한도

GMS 하루 요청 한도 수치는 공개 문서가 없고 콘솔(gms.ssafy.io)에서만 볼 수 있다. 그래서 스크립트는 한도를 *가정하지 않고*
응답을 보고 움직인다.

- `429`/`quota`/`RESOURCE_EXHAUSTED` 응답 → 같은 키로 20·40초 쉬고 재시도, 3번째면 그 키를 **오늘 소진**으로 표시하고 다른 키로 바꾼다.
- 모든 키가 소진되면 **KST 자정까지 기다렸다가 자동으로 이어간다**(창을 닫고 내일 다시 실행해도 같다).
- 키별 오늘 요청 수는 `output/quota_state.json`에 남는다(키 원문 대신 해시). 콘솔에서 확인한 한도를 알면 `.env`에
  `GMS_DAILY_REQUEST_LIMIT=숫자`를 넣으면 그 수에 도달하기 전에 스스로 키를 바꾼다.
- 한 요청에 10곳을 보내므로 장소 수 ÷ 10 이 필요한 요청 수다. 같은 키를 두 컴퓨터가 쓰면 요청 수는 합산되니
  콘솔 잔량은 그 기준으로 본다.

## 문제가 나면

메시지 앞에 종류가 붙는다.

- `[설정 문제]` — 덤프 경로, `.env` 키 없음, 키/모델명 오류(401/403/400). 고치고 다시 실행.
- `[오류]` — 네트워크나 응답 형식. 6번까지 자동 재시도한 뒤 멈춘 것이니 그냥 다시 실행하면 된다. 계속 나면 메시지를 공유.
- 아무 출력 없이 창이 바로 닫히면 Python이 없는 것. `python --version`으로 확인.

## DB에 넣기

각자 끝난 SQL 3개를 한 DB에 순서 상관없이 실행한다. 같은 장소가 있으면 지우고 다시 넣으므로 여러 번 실행해도 안전하다.

```bash
psql -U soomgil -d soomgil -f output/part-A/soomgil_place_tags_A.sql
```

Docker로 띄운 로컬 DB라면:

```bash
docker exec -i soomgil-postgres-1 psql -U soomgil -d soomgil < output/part-A/soomgil_place_tags_A.sql
```

## 테스트

```bash
python -m unittest test_place_tagging.py
```
