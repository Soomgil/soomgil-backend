#!/usr/bin/env python3
"""전국 장소 텍스트 태깅 — SSAFY 덤프의 모든 지역을 3파트(A/B/C)로 나눠 GMS로 태그를 뽑고 SQL을 만든다.

제주(area_code=39)는 이미 jeju-tagging으로 끝났으므로 기본 제외한다. 프롬프트·모델·선정 정책·SQL 형식은
jeju-tagging과 같아서(모듈을 그대로 가져다 씀) 결과 SQL을 같은 DB에 그대로 넣을 수 있다.

사용법(각자 한 파트만): run-A.bat / run-B.bat / run-C.bat 을 더블클릭. 끊기면 다시 더블클릭하면 이어서 한다.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import random
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

TOOL_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOL_DIR.parent / "jeju-tagging"))
import jeju_tagging as base  # noqa: E402  (프롬프트/스키마/검증/선정/SQL 헬퍼 재사용)

ROOT_DIR = TOOL_DIR.parents[2]
for _stream in (sys.stdout, sys.stderr):  # 한글이 콘솔 인코딩 때문에 깨지거나 예외로 죽지 않게
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass
OUTPUT_DIR = TOOL_DIR / "output"
PLACES_FILE = OUTPUT_DIR / "all_places.jsonl"
QUOTA_FILE = OUTPUT_DIR / "quota_state.json"
KST = timezone(timedelta(hours=9))

PART_LABELS = ("A", "B", "C")
EXCLUDED_AREA_CODES = {base.AREA_CODE_JEJU}
AREA_NAMES = {
    "1": "서울", "2": "인천", "3": "대전", "4": "대구", "5": "광주", "6": "부산", "7": "울산", "8": "세종",
    "31": "경기", "32": "강원", "33": "충북", "34": "충남", "35": "경북", "36": "경남", "37": "전북",
    "38": "전남", "39": "제주",
}
DEFAULT_BATCH_SIZE = 10
DEFAULT_DELAY = 2.0
SQL_FLUSH_EVERY = 20  # 묶음 N개마다 SQL 파일을 갱신해 중간에 꺼져도 최신 SQL이 남게 한다.


# ── 오류 분류 ──────────────────────────────────────────────────────────────
class ConfigError(RuntimeError):
    """설정 문제. 재시도해도 소용없으니 원인을 알려주고 멈춘다."""


class QuotaExhausted(RuntimeError):
    """이 키의 오늘 한도가 끝났다. 다른 키로 바꾸거나 자정까지 기다린다."""


QUOTA_PATTERN = re.compile(r"quota|RESOURCE_EXHAUSTED|rate ?limit|한도|too many requests", re.IGNORECASE)


def is_quota_error(status: int, body: str) -> bool:
    if status == 429:
        return True
    return bool(QUOTA_PATTERN.search(body or ""))


# ── 장소 추출(전국) ─────────────────────────────────────────────────────────
def extract_all_places(input_path: Path, destination: Path = PLACES_FILE) -> list[dict[str, object]]:
    if not input_path.exists():
        raise ConfigError(
            f"원본 SQL 덤프를 찾을 수 없습니다: {input_path}\n"
            "  → SSAFY_TRIP_Dump.sql 을 Downloads\\SSAFY_HOME_TRIP_202604\\ 에 두거나, "
            "run 파일에 덤프 파일을 드래그해서 실행하세요."
        )
    places = []
    for row in base.iter_attraction_rows(input_path):
        place = base.place_from_row(row)
        place["area_code"] = str(row["area_code"])
        places.append(place)
    places.sort(key=lambda item: int(str(item["content_id"])))
    seen: set[str] = set()
    unique = [p for p in places if not (str(p["content_id"]) in seen or seen.add(str(p["content_id"])))]
    destination.parent.mkdir(parents=True, exist_ok=True)
    base.write_jsonl_atomic(destination, unique)
    return unique


def ensure_places(input_path: Path) -> list[dict[str, object]]:
    if PLACES_FILE.exists():
        rows = base.read_jsonl(PLACES_FILE)
        if rows and "area_code" in rows[0]:
            return rows
    print("덤프에서 전국 장소를 추출합니다(처음 한 번, 1~2분)…", flush=True)
    return extract_all_places(input_path)


# ── 파트 나누기 ─────────────────────────────────────────────────────────────
def partition_by_area(places: list[dict[str, object]], part_count: int = 3,
                      excluded: set[str] = EXCLUDED_AREA_CODES) -> dict[str, list[str]]:
    """지역(area_code) 단위로 크게 나눈다. 장소 수가 많은 지역부터 가장 적게 쌓인 파트에 넣는다(결정적)."""
    counts: dict[str, int] = {}
    for place in places:
        code = str(place["area_code"])
        if code in excluded:
            continue
        counts[code] = counts.get(code, 0) + 1
    labels = list(PART_LABELS[:part_count])
    buckets: dict[str, list[str]] = {label: [] for label in labels}
    loads = {label: 0 for label in labels}
    for code, count in sorted(counts.items(), key=lambda item: (-item[1], int(item[0]) if item[0].isdigit() else 0)):
        target = min(labels, key=lambda label: (loads[label], label))
        buckets[target].append(code)
        loads[target] += count
    return buckets


def normalize_part(value: str) -> str:
    text = (value or "").strip().upper()
    numbers = {str(index + 1): label for index, label in enumerate(PART_LABELS)}
    text = numbers.get(text, text)
    if text not in PART_LABELS:
        raise ValueError(f"파트는 A/B/C(또는 1/2/3) 중 하나여야 합니다: {value!r}")
    return text


def split_pending(places: list[dict[str, object]], tagged_file: Path):
    completed = {str(row["content_id"]): row for row in base.read_jsonl(tagged_file)}
    pending = [place for place in places if str(place["content_id"]) not in completed]
    return pending, completed


# ── GMS 키 풀(일일 한도·키 교대·자정 리셋) ────────────────────────────────────
def key_fingerprint(key: str) -> str:
    return hashlib.sha256(key.encode("utf-8")).hexdigest()[:12]


class KeyPool:
    def __init__(self, keys: list[str], part: str, state_path: Path = QUOTA_FILE, clock=None,
                 daily_limit: int | None = None) -> None:
        if not keys:
            raise ConfigError("루트 .env 에 GMS_API_KEY 가 없습니다. (여러 개면 GMS_API_KEY_2, GMS_API_KEY_3 …)")
        self.keys = keys
        self.state_path = state_path
        self.clock = clock or (lambda: datetime.now(KST))
        self.daily_limit = daily_limit
        primary = PART_LABELS.index(part) % len(keys)
        self.order = keys[primary:] + keys[:primary]
        self.state: dict[str, dict[str, object]] = {}
        if state_path.exists():
            try:
                self.state = json.loads(state_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                self.state = {}

    # 상태는 키 원문 대신 지문(sha256 앞 12자리)으로 저장한다.
    def _entry(self, key: str) -> dict[str, object]:
        today = self.clock().date().isoformat()
        entry = self.state.setdefault(key_fingerprint(key), {})
        if entry.get("date") != today:
            entry.clear()
            entry.update({"date": today, "requests": 0, "exhausted_until": None})
        return entry

    def _save(self) -> None:
        self.state_path.parent.mkdir(parents=True, exist_ok=True)
        temp = self.state_path.with_suffix(".json.tmp")
        temp.write_text(json.dumps(self.state, ensure_ascii=False, indent=2), encoding="utf-8")
        temp.replace(self.state_path)

    def _next_midnight(self) -> datetime:
        now = self.clock()
        return (now + timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)

    def is_available(self, key: str) -> bool:
        entry = self._entry(key)
        until = entry.get("exhausted_until")
        if until and datetime.fromisoformat(str(until)) > self.clock():
            return False
        if self.daily_limit is not None and int(entry.get("requests", 0)) >= self.daily_limit:
            return False
        return True

    def current(self) -> str | None:
        for key in self.order:
            if self.is_available(key):
                return key
        return None

    def mark_exhausted(self, key: str) -> None:
        self._entry(key)["exhausted_until"] = self._next_midnight().isoformat()
        self._save()

    def record_request(self, key: str) -> None:
        entry = self._entry(key)
        entry["requests"] = int(entry.get("requests", 0)) + 1
        self._save()

    def requests_today(self, key: str) -> int:
        return int(self._entry(key).get("requests", 0))

    def wait_until_reset(self) -> timedelta:
        return self._next_midnight() - self.clock()

    def label(self, key: str) -> str:
        return f"키#{self.keys.index(key) + 1}"


def load_keys() -> list[str]:
    base.load_env(ROOT_DIR / ".env")
    keys: list[str] = []
    for name in ("GMS_API_KEY", "GMS_API_KEY_2", "GMS_API_KEY_3", "GMS_API_KEY_4"):
        value = os.getenv(name, "").strip()
        if value and value not in keys:
            keys.append(value)
    for value in os.getenv("GMS_API_KEYS", "").split(","):
        value = value.strip()
        if value and value not in keys:
            keys.append(value)
    return keys


def mask(text: str, keys: list[str]) -> str:
    for key in keys:
        if key:
            text = text.replace(key, "***")
    return text


# ── GMS 호출 ────────────────────────────────────────────────────────────────
class GmsClient:
    def __init__(self, keys: list[str]) -> None:
        self.keys = keys
        self.base_url = os.getenv(
            "GMS_GEMINI_BASE_URL", "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com"
        ).rstrip("/")
        self.api_version = os.getenv("GMS_GEMINI_API_VERSION", "v1beta").strip("/")
        # 제주 태깅과 같은 모델. DB의 model_name 과 맞추기 위해 기본값도 같다.
        self.model = os.getenv("GMS_CHAT_MODEL", "gpt-5.5").strip() or "gpt-5.5"

    def tag(self, key: str, places: list[dict[str, object]], tags: list[base.Tag], retries: int = 6):
        url = (
            f"{self.base_url}/{self.api_version}/models/{urllib.parse.quote(self.model, safe='-_.')}:streamGenerateContent"
            f"?alt=sse&key={urllib.parse.quote(key, safe='')}"
        )
        body = {
            "contents": [{"role": "user", "parts": [{"text": base.build_prompt(places, tags)}]}],
            "generationConfig": {
                "temperature": 0.2,
                "responseMimeType": "application/json",
                "responseSchema": base.response_schema(tags),
            },
        }
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        rate_limited = 0
        for attempt in range(retries):
            request = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"}, method="POST")
            try:
                with urllib.request.urlopen(request, timeout=180) as response:
                    raw = response.read().decode("utf-8")
                expected_ids = [str(place["content_id"]) for place in places]
                return base.validate_response(base.parse_sse_response(raw, expected_ids), places, tags)
            except urllib.error.HTTPError as exc:
                detail = ""
                try:
                    detail = exc.read().decode("utf-8", errors="replace")[:600]
                except Exception:  # noqa: BLE001
                    pass
                message = mask(f"HTTP {exc.code} {detail}", self.keys)
                if is_quota_error(exc.code, detail):
                    daily = re.search(r"daily|per ?day|일일|하루|quota", detail, re.IGNORECASE)
                    rate_limited += 1
                    if daily or rate_limited >= 3:
                        raise QuotaExhausted(message) from exc
                    wait = 20 * rate_limited
                    print(f"  잠시 한도(429). {wait}초 쉬고 같은 키로 다시 시도합니다.", flush=True)
                    time.sleep(wait)
                    continue
                if 400 <= exc.code < 500:
                    raise ConfigError(
                        f"GMS가 요청을 거절했습니다: {message}\n"
                        "  → 키가 틀렸거나(401/403) 모델명이 잘못됐을 수 있습니다. .env 의 GMS_API_KEY / GMS_CHAT_MODEL 을 확인하세요."
                    ) from exc
                self._sleep_before_retry(attempt, retries, message)
            except (urllib.error.URLError, TimeoutError, OSError) as exc:
                self._sleep_before_retry(attempt, retries, mask(f"네트워크 오류: {exc}", self.keys))
            except (KeyError, IndexError, json.JSONDecodeError, ValueError) as exc:
                self._sleep_before_retry(attempt, retries, mask(f"응답 형식 오류: {exc}", self.keys))
        raise RuntimeError(f"GMS 호출이 {retries}회 연속 실패했습니다. 잠시 후 다시 실행하세요.")

    @staticmethod
    def _sleep_before_retry(attempt: int, retries: int, message: str) -> None:
        if attempt + 1 >= retries:
            raise RuntimeError(message)
        delay = min(90.0, 3 * 2 ** attempt + random.random())
        print(f"  재시도 {attempt + 1}/{retries - 1}: {message[:160]} ({delay:.0f}초 후)", flush=True)
        time.sleep(delay)


# ── SQL 생성(jeju-tagging 형식 그대로) ───────────────────────────────────────
def generate_sql(rows: list[dict[str, object]], destination: Path, part: str) -> None:
    if not rows:
        return
    model = os.getenv("GMS_CHAT_MODEL", "gpt-5.5").strip() or "gpt-5.5"
    tags_by_code = {tag.code: tag for tag in base.load_tags()}
    areas = sorted({str(row.get("area_code")) for row in rows}, key=lambda code: int(code) if code.isdigit() else 0)
    area_text = ", ".join(f"{AREA_NAMES.get(code, code)}({code})" for code in areas)
    lines = [
        "-- Generated by backend/tools/place-tagging/place_tagging.py",
        f"-- Part {part}: text-only GMS tagging of places from SSAFY_TRIP_Dump.sql. areas: {area_text}",
        f"-- places: {len(rows)}, generated at {datetime.now(KST).isoformat(timespec='seconds')}",
        "-- 적용: psql -U soomgil -d soomgil -f <이 파일>   (같은 장소가 있으면 지우고 다시 넣으므로 여러 번 실행해도 안전)",
        "BEGIN;",
        "",
        "CREATE TEMP TABLE tagged_place_ids (external_place_id varchar(120) PRIMARY KEY) ON COMMIT DROP;",
        "INSERT INTO tagged_place_ids (external_place_id) VALUES",
        ",\n".join(f"  ({base.sql_literal(row['content_id'])})" for row in rows) + ";",
        "DELETE FROM preference.place_tag_enrichments enrichment",
        "USING tagged_place_ids target",
        "WHERE enrichment.provider = 'KTO' AND enrichment.external_place_id = target.external_place_id;",
        "",
    ]
    for row in rows:
        content_id = str(row["content_id"])
        enrichment_id = base.deterministic_uuid("jeju-enrichment-v1", content_id)  # 제주와 같은 규칙(재실행 시 동일 id)
        decisions, selected = base.selected_tags(row)
        lines.extend([
            "INSERT INTO preference.place_tag_enrichments (",
            "  id, provider, external_place_id, source_modified_at, source_hash, status,",
            "  model_provider, model_name, prompt_version, tag_dictionary_version,",
            "  selection_policy_version, candidate_count, selected_count, enriched_at",
            ") VALUES (",
            f"  '{enrichment_id}'::uuid, 'KTO', {base.sql_literal(content_id)}, NULL, {base.sql_literal(row['source_hash'])}, 'SUCCEEDED',",
            f"  'GOOGLE', {base.sql_literal(model)}, {base.sql_literal(base.PROMPT_VERSION)},",
            f"  {base.sql_literal(base.DICTIONARY_VERSION)}, {base.sql_literal(base.SELECTION_POLICY_VERSION)}, {len(decisions)}, {len(selected)}, now()",
            ");",
        ])
        for item in decisions:
            candidate_id = base.deterministic_uuid("jeju-candidate-v1", f"{content_id}:{item['code']}")
            tag_id = base.deterministic_uuid(base.DICTIONARY_VERSION, item["code"])
            lines.append(
                "INSERT INTO preference.place_tag_enrichment_candidates "
                "(id, enrichment_id, candidate_code, matched_tag_id, confidence, weight, selection_score, status, rationale) "
                f"VALUES ('{candidate_id}'::uuid, '{enrichment_id}'::uuid, {base.sql_literal(item['code'])}, "
                f"'{tag_id}'::uuid, {item['confidence']}, {item['weight']}, {item['score']}, "
                f"{base.sql_literal(item['status'])}, {base.sql_literal(item.get('rationale', ''))});"
            )
        for rank, item in enumerate(selected, 1):
            if item["code"] not in tags_by_code:
                continue
            tag_id = base.deterministic_uuid(base.DICTIONARY_VERSION, item["code"])
            lines.append(
                "INSERT INTO preference.place_tag_enrichment_tags "
                "(enrichment_id, tag_id, confidence, weight, preference_discrimination_snapshot, "
                "selection_score, rank_order, tag_statistic_run_id, rationale) "
                f"VALUES ('{enrichment_id}'::uuid, '{tag_id}'::uuid, {item['confidence']}, {item['weight']}, "
                f"0.500000, {item['score']}, {rank}, NULL, {base.sql_literal(item.get('rationale', ''))});"
            )
        lines.append("")
    lines.extend(["COMMIT;", ""])
    destination.parent.mkdir(parents=True, exist_ok=True)
    temp = destination.with_suffix(".sql.tmp")
    temp.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    temp.replace(destination)


# ── 출력 도우미 ─────────────────────────────────────────────────────────────
def describe_parts(places: list[dict[str, object]], parts: dict[str, list[str]], current: str | None) -> None:
    counts: dict[str, int] = {}
    for place in places:
        counts[str(place["area_code"])] = counts.get(str(place["area_code"]), 0) + 1
    print("파트 나누기(지역 단위, 장소 수 균형):")
    for label, codes in parts.items():
        total = sum(counts.get(code, 0) for code in codes)
        names = ", ".join(f"{AREA_NAMES.get(code, code)} {counts.get(code, 0):,}" for code in codes)
        marker = " ◀ 이 컴퓨터" if label == current else ""
        print(f"  [{label}] {total:,}곳 — {names}{marker}")
    excluded = sum(counts.get(code, 0) for code in EXCLUDED_AREA_CODES)
    if excluded:
        print(f"  (제외) 제주 {excluded:,}곳 — 이미 태깅 완료")


def format_duration(seconds: float) -> str:
    seconds = max(0, int(seconds))
    hours, rest = divmod(seconds, 3600)
    minutes = rest // 60
    return f"{hours}시간 {minutes}분" if hours else f"{minutes}분"


# ── 실행 ────────────────────────────────────────────────────────────────────
def run_part(part: str, input_path: Path, batch_size: int, delay: float, daily_limit: int | None) -> int:
    keys = load_keys()
    pool = KeyPool(keys, part, daily_limit=daily_limit)
    client = GmsClient(keys)
    places = ensure_places(input_path)
    parts = partition_by_area(places)
    describe_parts(places, parts, part)

    my_codes = set(parts[part])
    my_places = [place for place in places if str(place["area_code"]) in my_codes]
    part_dir = OUTPUT_DIR / f"part-{part}"
    tagged_file = part_dir / "tagged.jsonl"
    sql_file = part_dir / f"soomgil_place_tags_{part}.sql"
    pending, completed = split_pending(my_places, tagged_file)
    tags = base.load_tags()
    print(f"\n[{part}] 장소 {len(my_places):,}곳 / 완료 {len(completed):,} / 남음 {len(pending):,}"
          f" · 모델 {client.model} · 키 {len(keys)}개(기본 {pool.label(pool.order[0])})"
          f" · 한 요청 {batch_size}곳, 요청 사이 {delay:g}초", flush=True)
    if daily_limit:
        print(f"  하루 요청 한도 {daily_limit:,}회/키로 계산합니다(.env GMS_DAILY_REQUEST_LIMIT).")
    else:
        print("  GMS 하루 한도 값은 공개돼 있지 않아 429/quota 응답을 보고 자동으로 키를 바꾸거나 자정(KST)까지 기다립니다.")

    def ordered() -> list[dict[str, object]]:
        return [completed[str(p["content_id"])] for p in my_places if str(p["content_id"]) in completed]

    batches = base.chunks(pending, batch_size)
    done = len(completed)
    started = time.monotonic()
    sent = 0
    since_flush = 0
    try:
        for batch in batches:
            while True:
                key = pool.current()
                if key is None:
                    wait = pool.wait_until_reset()
                    print(f"\n모든 키의 오늘 한도가 끝났습니다. 자정(KST)까지 {format_duration(wait.total_seconds())} 기다렸다가 자동으로 이어갑니다."
                          " (창을 닫고 내일 다시 실행해도 됩니다)", flush=True)
                    generate_sql(ordered(), sql_file, part)
                    time.sleep(min(wait.total_seconds() + 5, 1800))
                    continue
                try:
                    results = client.tag(key, batch, tags)
                    pool.record_request(key)
                    break
                except QuotaExhausted as exc:
                    pool.record_request(key)
                    pool.mark_exhausted(key)
                    print(f"  {pool.label(key)} 오늘 한도 소진: {str(exc)[:120]}", flush=True)
            base.append_results(tagged_file, results)
            for result in results:
                completed[str(result["content_id"])] = result
            done += len(results)
            sent += 1
            since_flush += 1
            elapsed = time.monotonic() - started
            remaining_batches = len(batches) - sent
            eta = format_duration(elapsed / sent * remaining_batches) if sent else "?"
            print(f"  [{part}] {done:,}/{len(my_places):,} ({done * 100 / len(my_places):.1f}%)"
                  f" · 이번 실행 {sent}회 · {pool.label(key)} 오늘 {pool.requests_today(key):,}회 · 남은 예상 {eta}", flush=True)
            if since_flush >= SQL_FLUSH_EVERY:
                generate_sql(ordered(), sql_file, part)
                since_flush = 0
            if delay > 0 and remaining_batches:
                time.sleep(delay)
    except KeyboardInterrupt:
        generate_sql(ordered(), sql_file, part)
        print(f"\n중단했습니다. 지금까지 {done:,}/{len(my_places):,}곳 저장됨. 같은 파일을 다시 실행하면 이어서 합니다.\n"
              f"현재까지의 SQL: {sql_file}", file=sys.stderr)
        return 130

    rows = ordered()
    generate_sql(rows, sql_file, part)
    if len(rows) == len(my_places):
        print(f"\n[완료] [{part}] {len(rows):,}곳. SQL: {sql_file}")
        print('   DB에 넣기: psql -U soomgil -d soomgil -f "' + str(sql_file) + '"')
    else:
        print(f"\n[{part}] {len(rows):,}/{len(my_places):,}곳까지 저장. 다시 실행하면 이어서 합니다. SQL: {sql_file}")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = result.add_subparsers(dest="command", required=True)
    default_input = Path.home() / "Downloads" / "SSAFY_HOME_TRIP_202604" / "SSAFY_TRIP_Dump.sql"
    for name in ("plan", "run"):
        command = sub.add_parser(name)
        command.add_argument("--input", type=Path, default=Path(os.getenv("TRIP_DUMP", default_input)),
                             help="SSAFY_TRIP_Dump.sql 경로(기본: Downloads\\SSAFY_HOME_TRIP_202604\\, 또는 TRIP_DUMP 환경변수)")
        if name == "run":
            command.add_argument("part", help="A/B/C 또는 1/2/3")
            command.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
            command.add_argument("--delay", type=float, default=DEFAULT_DELAY, help="요청 사이 대기 초")
    return result


def main() -> int:
    args = parser().parse_args()
    if args.command == "plan":
        places = ensure_places(args.input)
        describe_parts(places, partition_by_area(places), None)
        return 0
    part = normalize_part(args.part)
    if not 1 <= args.batch_size <= 20:
        raise ConfigError("batch-size는 1~20이어야 합니다.")
    base.load_env(ROOT_DIR / ".env")
    limit_text = os.getenv("GMS_DAILY_REQUEST_LIMIT", "").strip()
    daily_limit = int(limit_text) if limit_text.isdigit() and int(limit_text) > 0 else None
    return run_part(part, args.input, args.batch_size, args.delay, daily_limit)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise SystemExit(130)
    except ConfigError as exc:
        print(f"\n[설정 문제] {exc}", file=sys.stderr)
        raise SystemExit(2)
    except Exception as exc:  # noqa: BLE001
        print(f"\n[오류] {mask(str(exc), load_keys())}\n   다시 실행하면 저장된 곳부터 이어서 합니다. 계속 나면 이 메시지를 공유해 주세요.",
              file=sys.stderr)
        raise SystemExit(1)
