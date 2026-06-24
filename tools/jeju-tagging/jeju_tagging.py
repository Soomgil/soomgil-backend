#!/usr/bin/env python3
"""Extract Jeju places from a MySQL dump and tag them through GMS Gemini."""

from __future__ import annotations

import argparse
import concurrent.futures
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
import uuid
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Iterable, Iterator


TOOL_DIR = Path(__file__).resolve().parent
BACKEND_DIR = TOOL_DIR.parents[1]
ROOT_DIR = TOOL_DIR.parents[2]
OUTPUT_DIR = TOOL_DIR / "output"
PLACES_FILE = OUTPUT_DIR / "jeju_places.jsonl"
TAGGED_FILE = OUTPUT_DIR / "tagged.jsonl"
SAMPLE_FILE = OUTPUT_DIR / "sample.json"
SQL_FILE = OUTPUT_DIR / "soomgil_jeju_place_tags.sql"
TAG_MIGRATION = BACKEND_DIR / "src/main/resources/db/migration/V28__create_preference_tag_seed.sql"

AREA_CODE_JEJU = "39"
PROMPT_VERSION = "jeju-place-text-tagging-v1"
DICTIONARY_VERSION = "preference-tags-v1"
SELECTION_POLICY_VERSION = "preference-selection-v1"
MINIMUM_CONFIDENCE = Decimal("0.55")
MAXIMUM_SELECTED_TAGS = 10

ATTRACTION_COLUMNS = (
    "no", "content_id", "title", "content_type_id", "area_code", "si_gun_gu_code",
    "first_image1", "first_image2", "map_level", "latitude", "longitude", "tel",
    "addr1", "addr2", "homepage", "overview",
)

CONTENT_TYPES = {
    "12": "관광지", "14": "문화시설", "15": "축제/공연/행사", "25": "여행코스",
    "28": "레포츠", "32": "숙박", "38": "쇼핑", "39": "음식점",
}


@dataclass(frozen=True)
class Tag:
    code: str
    name: str


def load_env(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8-sig").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def parse_mysql_values(text: str, start: int) -> tuple[list[list[object]], int]:
    """Parse tuples after a MySQL VALUES keyword, including escaped strings and newlines."""
    rows: list[list[object]] = []
    row: list[object] | None = None
    token: list[str] = []
    in_string = False
    escaped = False
    quoted = False
    i = start

    def finish_value() -> None:
        nonlocal token, quoted
        assert row is not None
        raw = "".join(token)
        if quoted:
            value: object = raw
        else:
            stripped = raw.strip()
            value = None if stripped.upper() == "NULL" else stripped
        row.append(value)
        token = []
        quoted = False

    while i < len(text):
        char = text[i]
        if in_string:
            if escaped:
                replacements = {"n": "\n", "r": "\r", "t": "\t", "0": "\0", "Z": "\x1a"}
                token.append(replacements.get(char, char))
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == "'":
                if i + 1 < len(text) and text[i + 1] == "'":
                    token.append("'")
                    i += 1
                else:
                    in_string = False
            else:
                token.append(char)
        elif char == "'":
            in_string = True
            quoted = True
        elif char == "(":
            if row is None:
                row = []
                token = []
                quoted = False
        elif char == "," and row is not None:
            finish_value()
        elif char == ")" and row is not None:
            finish_value()
            rows.append(row)
            row = None
        elif char == ";" and row is None:
            return rows, i + 1
        elif row is not None:
            token.append(char)
        i += 1
    raise ValueError("unterminated INSERT statement")


def iter_attraction_rows(path: Path) -> Iterator[dict[str, object]]:
    text = path.read_text(encoding="utf-8-sig")
    marker = "INSERT INTO `attractions` VALUES"
    position = 0
    while True:
        found = text.find(marker, position)
        if found < 0:
            return
        rows, position = parse_mysql_values(text, found + len(marker))
        for values in rows:
            if len(values) != len(ATTRACTION_COLUMNS):
                raise ValueError(f"expected {len(ATTRACTION_COLUMNS)} columns, got {len(values)}")
            yield dict(zip(ATTRACTION_COLUMNS, values))


def clean_text(value: object, limit: int) -> str:
    if value is None:
        return ""
    text = re.sub(r"<[^>]+>", " ", str(value))
    text = re.sub(r"\s+", " ", text).strip()
    return text[:limit]


def place_from_row(row: dict[str, object]) -> dict[str, object]:
    place = {
        "content_id": str(row["content_id"]),
        "title": clean_text(row["title"], 200),
        "category": CONTENT_TYPES.get(str(row["content_type_id"]), str(row["content_type_id"])),
        "address": clean_text(" ".join(filter(None, [str(row["addr1"] or ""), str(row["addr2"] or "")])), 300),
        "overview": clean_text(row["overview"], 1800),
    }
    canonical = json.dumps(place, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    place["source_hash"] = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    return place


def extract_places(input_path: Path, destination: Path = PLACES_FILE) -> list[dict[str, object]]:
    if not input_path.exists():
        raise FileNotFoundError(f"원본 SQL을 찾을 수 없습니다: {input_path}")
    places = [place_from_row(row) for row in iter_attraction_rows(input_path) if str(row["area_code"]) == AREA_CODE_JEJU]
    places.sort(key=lambda item: int(str(item["content_id"])))
    seen: set[str] = set()
    unique = [place for place in places if not (str(place["content_id"]) in seen or seen.add(str(place["content_id"])))]
    destination.parent.mkdir(parents=True, exist_ok=True)
    write_jsonl_atomic(destination, unique)
    return unique


def load_tags() -> list[Tag]:
    text = TAG_MIGRATION.read_text(encoding="utf-8")
    pattern = re.compile(r"\('([^']+)',\s*'([^']+)',\s*'[^']+',\s*'TAG',\s*true,\s*\d+\)")
    tags = [Tag(code, name) for code, name in pattern.findall(text)]
    if not tags:
        raise RuntimeError(f"태그 사전을 읽지 못했습니다: {TAG_MIGRATION}")
    return tags


def write_jsonl_atomic(path: Path, rows: Iterable[dict[str, object]]) -> None:
    temp = path.with_suffix(path.suffix + ".tmp")
    with temp.open("w", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
        handle.flush()
        os.fsync(handle.fileno())
    temp.replace(path)


def read_jsonl(path: Path) -> list[dict[str, object]]:
    if not path.exists():
        return []
    rows = []
    with path.open(encoding="utf-8") as handle:
        for number, line in enumerate(handle, 1):
            if line.strip():
                try:
                    rows.append(json.loads(line))
                except json.JSONDecodeError as exc:
                    raise ValueError(f"{path}:{number} JSON 오류") from exc
    return rows


def chunks(items: list[dict[str, object]], size: int) -> list[list[dict[str, object]]]:
    return [items[index:index + size] for index in range(0, len(items), size)]


def build_prompt(places: list[dict[str, object]], tags: list[Tag]) -> str:
    dictionary = ", ".join(f"{tag.code}={tag.name}" for tag in tags)
    inputs = [
        {
            "content_id": place["content_id"],
            "장소명": place["title"],
            "분류": place["category"],
            "주소": place["address"],
            "설명": place["overview"],
        }
        for place in places
    ]
    return (
        "당신은 한국 여행 장소의 취향 태그 분류기입니다. 이미지 없이 제공된 텍스트만 사용하세요.\n"
        "각 장소를 직접 구분하는 태그만 허용 목록에서 최대 10개 선택하세요. 근거가 약하면 제외하세요.\n"
        "confidence는 텍스트 근거의 확실성, weight는 그 태그가 장소의 핵심 특징인 정도이며 둘 다 0~1입니다.\n"
        "반드시 모든 content_id를 한 번씩 포함하고 허용되지 않은 코드는 만들지 마세요.\n\n"
        f"허용 태그: {dictionary}\n\n"
        f"장소 목록: {json.dumps(inputs, ensure_ascii=False, separators=(',', ':'))}"
    )


def response_schema(tags: list[Tag]) -> dict[str, object]:
    return {
        "type": "ARRAY",
        "items": {
            "type": "OBJECT",
            "required": ["content_id", "tags"],
            "properties": {
                "content_id": {"type": "STRING"},
                "tags": {
                    "type": "ARRAY", "maxItems": 10,
                    "items": {
                        "type": "OBJECT",
                        "required": ["code", "confidence", "weight"],
                        "properties": {
                            "code": {"type": "STRING"},
                            "confidence": {"type": "NUMBER", "minimum": 0, "maximum": 1},
                            "weight": {"type": "NUMBER", "minimum": 0, "maximum": 1},
                        },
                    },
                },
            },
        },
    }


class GmsClient:
    def __init__(self) -> None:
        load_env(ROOT_DIR / ".env")
        self.api_key = os.getenv("GMS_API_KEY", "").strip()
        self.base_url = os.getenv(
            "GMS_GEMINI_BASE_URL", "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com"
        ).rstrip("/")
        self.api_version = os.getenv("GMS_GEMINI_API_VERSION", "v1beta").strip("/")
        self.model = os.getenv("GEMINI_CHAT_MODEL", "gemini-2.5-flash-lite").strip()
        if not self.api_key:
            raise RuntimeError("루트 .env에 GMS_API_KEY가 필요합니다.")
        if not self.model.startswith("gemini-2.5"):
            raise RuntimeError(f"이번 작업은 Gemini 2.5 모델만 허용합니다: {self.model}")

    def tag(self, places: list[dict[str, object]], tags: list[Tag], retries: int = 30) -> list[dict[str, object]]:
        url = (
            f"{self.base_url}/{self.api_version}/models/{urllib.parse.quote(self.model, safe='-_.')}:streamGenerateContent"
            f"?alt=sse&key={urllib.parse.quote(self.api_key, safe='')}"
        )
        body = {
            "contents": [{"role": "user", "parts": [{"text": build_prompt(places, tags)}]}],
            "generationConfig": {
                "temperature": 0.2,
                "responseMimeType": "application/json",
                "responseSchema": response_schema(tags),
            },
        }
        request = urllib.request.Request(
            url,
            data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        for attempt in range(retries):
            try:
                with urllib.request.urlopen(request, timeout=120) as response:
                    raw = response.read().decode("utf-8")
                expected_ids = [str(place["content_id"]) for place in places]
                return validate_response(parse_sse_response(raw, expected_ids), places, tags)
            except (urllib.error.URLError, urllib.error.HTTPError, KeyError, IndexError, json.JSONDecodeError, ValueError) as exc:
                if isinstance(exc, urllib.error.HTTPError) and 400 <= exc.code < 500 and exc.code != 429:
                    raise RuntimeError(f"GMS 요청 설정 오류: {safe_error(exc)}") from exc
                if attempt + 1 >= retries:
                    raise RuntimeError(f"GMS 호출이 {retries}회 실패했습니다: {safe_error(exc)}") from exc
                delay = min(60.0, 2 ** attempt + random.random())
                print(f"  재시도 {attempt + 1}/{retries - 1}: {safe_error(exc)} ({delay:.1f}초 후)", flush=True)
                time.sleep(delay)
        raise AssertionError("unreachable")


def parse_json_parts(parts: object) -> object:
    if not isinstance(parts, list):
        raise ValueError("Gemini 응답 parts가 배열이 아닙니다")
    errors = []
    for part in reversed(parts):
        if not isinstance(part, dict) or not isinstance(part.get("text"), str):
            continue
        text = part["text"].strip()
        if text.startswith("```"):
            text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text, flags=re.IGNORECASE)
        if not text:
            continue
        try:
            return json.loads(text)
        except json.JSONDecodeError as exc:
            errors.append(f"{exc.msg} at {exc.pos}; prefix={text[:80]!r}")
    raise ValueError("Gemini JSON 본문을 찾지 못했습니다: " + "; ".join(errors[:2]))


def parse_sse_response(raw: str, expected_ids: list[str]) -> object:
    text_parts = []
    for line in raw.splitlines():
        if not line.startswith("data:"):
            continue
        event = json.loads(line[5:].strip())
        parts = event.get("candidates", [{}])[0].get("content", {}).get("parts", [])
        text_parts.extend(part.get("text", "") for part in parts if isinstance(part, dict))
    text = "".join(text_parts).strip()
    if not text.startswith("["):
        positions = [(text.find(f'"{content_id}"'), content_id) for content_id in expected_ids]
        positions = [(position, content_id) for position, content_id in positions if position >= 0]
        if positions:
            position, _ = min(positions)
            colon = text.rfind(":", 0, position)
            if colon >= 0:
                text = '[{"content_id"' + text[colon:]
    return json.loads(text)


def safe_error(error: BaseException) -> str:
    if isinstance(error, urllib.error.HTTPError):
        try:
            detail = error.read().decode("utf-8", errors="replace")[:500]
        except Exception:
            detail = ""
        return f"HTTP {error.code} {detail}".replace(os.getenv("GMS_API_KEY", ""), "***")
    return str(error).replace(os.getenv("GMS_API_KEY", ""), "***")


def validate_response(payload: object, places: list[dict[str, object]], tags: list[Tag]) -> list[dict[str, object]]:
    if not isinstance(payload, list):
        raise ValueError("응답이 배열이 아닙니다")
    expected = {str(place["content_id"]): place for place in places}
    allowed = {tag.code for tag in tags}
    found: dict[str, dict[str, object]] = {}
    for result in payload:
        if not isinstance(result, dict):
            raise ValueError("장소 응답 형식이 잘못됐습니다")
        content_id = str(result.get("content_id", ""))
        if content_id not in expected or content_id in found:
            raise ValueError(f"알 수 없거나 중복된 content_id: {content_id}")
        normalized_tags = []
        seen_codes = set()
        for item in result.get("tags", [])[:MAXIMUM_SELECTED_TAGS]:
            code = str(item.get("code", ""))
            if code not in allowed or code in seen_codes:
                continue
            seen_codes.add(code)
            confidence = min(1.0, max(0.0, float(item.get("confidence", 0))))
            weight = min(1.0, max(0.0, float(item.get("weight", 0))))
            normalized_tags.append({
                "code": code,
                "confidence": round(confidence, 4),
                "weight": round(weight, 4),
                "rationale": "",
            })
        place = expected[content_id]
        found[content_id] = {**place, "tags": normalized_tags}
    if set(found) != set(expected):
        raise ValueError(f"누락 content_id: {sorted(set(expected) - set(found))}")
    return [found[str(place["content_id"])] for place in places]


def append_results(path: Path, rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


def tag_all(places: list[dict[str, object]], workers: int, batch_size: int, delay: float) -> list[dict[str, object]]:
    completed_rows = read_jsonl(TAGGED_FILE)
    completed = {str(row["content_id"]): row for row in completed_rows}
    pending = [place for place in places if str(place["content_id"]) not in completed]
    tags = load_tags()
    batches = chunks(pending, batch_size)
    print(f"제주 장소 {len(places)}개 / 완료 {len(completed)}개 / 남음 {len(pending)}개", flush=True)
    if not batches:
        return [completed[str(place["content_id"])] for place in places]

    def process(batch: list[dict[str, object]]) -> list[dict[str, object]]:
        result = GmsClient().tag(batch, tags)
        if delay > 0:
            time.sleep(delay)
        return result

    done_count = len(completed)
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
        futures = {executor.submit(process, batch): batch for batch in batches}
        try:
            for future in concurrent.futures.as_completed(futures):
                results = future.result()
                append_results(TAGGED_FILE, results)
                for result in results:
                    completed[str(result["content_id"])] = result
                done_count += len(results)
                print(f"  진행 {done_count}/{len(places)} ({done_count * 100 / len(places):.1f}%)", flush=True)
        except KeyboardInterrupt:
            print("\n중단했습니다. 같은 명령을 실행하면 저장된 다음 장소부터 이어집니다.", file=sys.stderr)
            executor.shutdown(wait=False, cancel_futures=True)
            raise
    ordered = [completed[str(place["content_id"])] for place in places]
    write_jsonl_atomic(TAGGED_FILE, ordered)
    return ordered


def decimal(value: object) -> Decimal:
    return Decimal(str(value)).quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)


def sql_literal(value: object) -> str:
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def deterministic_uuid(namespace: str, value: str) -> str:
    return str(uuid.UUID(hashlib.md5(f"{namespace}:{value}".encode("utf-8")).hexdigest()))


def selected_tags(row: dict[str, object]) -> tuple[list[dict[str, object]], list[dict[str, object]]]:
    decisions = []
    for tag in row.get("tags", []):
        confidence = decimal(tag["confidence"])
        weight = decimal(tag["weight"])
        score = (confidence * Decimal("0.50") + weight * Decimal("0.30") + Decimal("0.10")).quantize(
            Decimal("0.000001"), rounding=ROUND_HALF_UP
        )
        decisions.append({**tag, "confidence": confidence, "weight": weight, "score": score})
    decisions.sort(key=lambda item: (-item["score"], -item["confidence"], -item["weight"], item["code"]))
    selected = []
    for item in decisions:
        if item["confidence"] < MINIMUM_CONFIDENCE:
            item["status"] = "REJECTED_LOW_CONFIDENCE"
        elif len(selected) >= MAXIMUM_SELECTED_TAGS:
            item["status"] = "REJECTED_LIMIT"
        else:
            item["status"] = "SELECTED"
            selected.append(item)
    return decisions, selected


def generate_sql(rows: list[dict[str, object]], destination: Path = SQL_FILE) -> None:
    tags_by_code = {tag.code: tag for tag in load_tags()}
    lines = [
        "-- Generated by backend/tools/jeju-tagging/jeju_tagging.py",
        "-- Text-only Gemini 2.5 tagging of Jeju places from SSAFY_TRIP_Dump.sql.",
        "BEGIN;",
        "",
        "CREATE TEMP TABLE jeju_tagged_place_ids (external_place_id varchar(120) PRIMARY KEY) ON COMMIT DROP;",
        "INSERT INTO jeju_tagged_place_ids (external_place_id) VALUES",
        ",\n".join(f"  ({sql_literal(row['content_id'])})" for row in rows) + ";",
        "DELETE FROM preference.place_tag_enrichments enrichment",
        "USING jeju_tagged_place_ids target",
        "WHERE enrichment.provider = 'KTO' AND enrichment.external_place_id = target.external_place_id;",
        "",
    ]
    for row in rows:
        content_id = str(row["content_id"])
        enrichment_id = deterministic_uuid("jeju-enrichment-v1", content_id)
        decisions, selected = selected_tags(row)
        lines.extend([
            "INSERT INTO preference.place_tag_enrichments (",
            "  id, provider, external_place_id, source_modified_at, source_hash, status,",
            "  model_provider, model_name, prompt_version, tag_dictionary_version,",
            "  selection_policy_version, candidate_count, selected_count, enriched_at",
            ") VALUES (",
            f"  '{enrichment_id}'::uuid, 'KTO', {sql_literal(content_id)}, NULL, {sql_literal(row['source_hash'])}, 'SUCCEEDED',",
            f"  'GOOGLE', {sql_literal(os.getenv('GEMINI_CHAT_MODEL', 'gemini-2.5-flash-lite'))}, {sql_literal(PROMPT_VERSION)},",
            f"  {sql_literal(DICTIONARY_VERSION)}, {sql_literal(SELECTION_POLICY_VERSION)}, {len(decisions)}, {len(selected)}, now()",
            ");",
        ])
        for index, item in enumerate(decisions):
            candidate_id = deterministic_uuid("jeju-candidate-v1", f"{content_id}:{item['code']}")
            tag_id = deterministic_uuid(DICTIONARY_VERSION, item["code"])
            lines.append(
                "INSERT INTO preference.place_tag_enrichment_candidates "
                "(id, enrichment_id, candidate_code, matched_tag_id, confidence, weight, selection_score, status, rationale) "
                f"VALUES ('{candidate_id}'::uuid, '{enrichment_id}'::uuid, {sql_literal(item['code'])}, "
                f"'{tag_id}'::uuid, {item['confidence']}, {item['weight']}, {item['score']}, "
                f"{sql_literal(item['status'])}, {sql_literal(item.get('rationale', ''))});"
            )
        for rank, item in enumerate(selected, 1):
            if item["code"] not in tags_by_code:
                continue
            tag_id = deterministic_uuid(DICTIONARY_VERSION, item["code"])
            lines.append(
                "INSERT INTO preference.place_tag_enrichment_tags "
                "(enrichment_id, tag_id, confidence, weight, preference_discrimination_snapshot, "
                "selection_score, rank_order, tag_statistic_run_id, rationale) "
                f"VALUES ('{enrichment_id}'::uuid, '{tag_id}'::uuid, {item['confidence']}, {item['weight']}, "
                f"0.500000, {item['score']}, {rank}, NULL, {sql_literal(item.get('rationale', ''))});"
            )
        lines.append("")
    lines.extend(["COMMIT;", ""])
    destination.parent.mkdir(parents=True, exist_ok=True)
    temp = destination.with_suffix(".sql.tmp")
    with temp.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(lines))
    temp.replace(destination)


def ensure_places(input_path: Path) -> list[dict[str, object]]:
    if not PLACES_FILE.exists():
        return extract_places(input_path)
    return read_jsonl(PLACES_FILE)


def print_sample(rows: list[dict[str, object]]) -> None:
    tags = {tag.code: tag.name for tag in load_tags()}
    for row in rows:
        labels = ", ".join(
            f"{tags.get(item['code'], item['code'])}({float(item['confidence']):.2f})" for item in row["tags"]
        )
        print(f"- {row['title']} [{row['content_id']}]: {labels or '태그 없음'}")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    subparsers = result.add_subparsers(dest="command", required=True)
    for name in ("extract", "sample", "run"):
        command = subparsers.add_parser(name)
        command.add_argument("--input", type=Path, required=True, help="SSAFY_TRIP_Dump.sql 경로")
        if name == "sample":
            command.add_argument("--limit", type=int, default=5)
        if name == "run":
            command.add_argument("--workers", type=int, default=1)
            command.add_argument("--batch-size", type=int, default=5)
            command.add_argument("--delay", type=float, default=12.0, help="요청 묶음 사이 대기 초")
    subparsers.add_parser("dump")
    return result


def main() -> int:
    args = parser().parse_args()
    if args.command == "extract":
        places = extract_places(args.input)
        print(f"제주 장소 {len(places)}개를 {PLACES_FILE}에 저장했습니다.")
        return 0
    if args.command == "sample":
        places = ensure_places(args.input)[:args.limit]
        results = GmsClient().tag(places, load_tags())
        SAMPLE_FILE.parent.mkdir(parents=True, exist_ok=True)
        SAMPLE_FILE.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
        print_sample(results)
        print(f"샘플 결과: {SAMPLE_FILE}")
        return 0
    if args.command == "run":
        if args.workers < 1 or args.batch_size < 1 or args.batch_size > 20:
            raise ValueError("workers는 1 이상, batch-size는 1~20이어야 합니다.")
        places = ensure_places(args.input)
        results = tag_all(places, args.workers, args.batch_size, args.delay)
        generate_sql(results)
        print(f"완료: {SQL_FILE}")
        return 0
    if args.command == "dump":
        rows = read_jsonl(TAGGED_FILE)
        if not rows:
            raise RuntimeError(f"태깅 결과가 없습니다: {TAGGED_FILE}")
        generate_sql(rows)
        print(f"완료: {SQL_FILE}")
        return 0
    return 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise SystemExit(130)
    except Exception as exc:
        print(f"오류: {safe_error(exc)}", file=sys.stderr)
        raise SystemExit(1)
