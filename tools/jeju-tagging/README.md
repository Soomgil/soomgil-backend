# Jeju place text tagging

SSAFY MySQL dump에서 제주도(`area_code=39`) 장소만 추출하고, 이미지 없이 GMS Gemini로
취향 태그를 생성한 뒤 Soomgil PostgreSQL용 SQL dump를 만든다.

## 빠른 실행

macOS:

```bash
./run-mac.command
```

Windows:

```bat
run-windows.bat
```

인자로 원본 SQL 경로를 넘길 수도 있다.

```bash
./run-mac.command /path/to/SSAFY_TRIP_Dump.sql
```

실행기는 루트 `.env`의 `GMS_API_KEY`, `GMS_GEMINI_BASE_URL`,
`GMS_GEMINI_API_VERSION`, `GMS_CHAT_MODEL`을 사용한다. 기본 모델은
`gpt-5.5`다.

처음 실행하면 제주 장소를 추출한 뒤 아직 처리하지 않은 장소만 5개씩 태깅한다. 진행 결과는
`output/tagged.jsonl`에 묶음마다 저장되므로 중간에 종료해도 같은 명령으로 이어서 실행할
수 있다. 완료 후 `output/soomgil_jeju_place_tags.sql`이 생성된다.

## 개별 명령

```bash
python3 jeju_tagging.py extract --input /path/to/SSAFY_TRIP_Dump.sql
python3 jeju_tagging.py sample --input /path/to/SSAFY_TRIP_Dump.sql --limit 5
python3 jeju_tagging.py run --input /path/to/SSAFY_TRIP_Dump.sql
python3 jeju_tagging.py dump
python3 -m unittest test_jeju_tagging.py
```

`sample` 결과는 별도 파일에 저장되어 전체 진행률에 포함되지 않는다. 기본적으로 GMS 부하를
피해 요청 사이에 12초 기다린다. 동시 요청 수는 `--workers`, 한 요청의 장소 수는
`--batch-size`, 대기 시간은 `--delay`로 조절할 수 있다.
