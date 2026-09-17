"""관광 데이터 전용 seed 도구. Docker PostgreSQL 컨테이너의 psql을 사용한다.
export/import는 관광공사 API를 호출하지 않는다. import는 단일 트랜잭션이다.
"""
import argparse
import json
import subprocess
from pathlib import Path

TABLES = {
    "contenttypes": ["content_type_id", "content_type_name"],
    "attractions": ["content_id", "title", "content_type_id", "area_code", "si_gun_gu_code", "first_image1", "first_image2", "latitude", "longitude", "tel", "addr1", "addr2", "homepage", "overview", "source_hash", "source_modified_at", "imported_at"],
    "attraction_images": ["source_provider", "source_type", "original_url", "public_url", "display_order", "width", "height", "checksum_sha256", "is_active", "created_at", "updated_at"],
    "kto_responses": ["request_key", "payload", "fetched_at", "source_modified_at"],
}

def psql(args, sql):
    command = ["docker", "exec", "-i", args.container, "psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1", "-U", args.user, "-d", args.database]
    result = subprocess.run(command, input=sql, text=True, encoding="utf-8", capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr)
    return result.stdout.strip()

def json_sql(value):
    encoded = json.dumps(value, ensure_ascii=False).encode("utf-8").hex()
    return "convert_from(decode('" + encoded + "','hex'),'UTF8')::jsonb"

def deduplicate(table, rows):
    keys = {"contenttypes": ["content_type_id"], "attractions": ["content_id"], "attraction_images": ["content_id", "public_url"], "kto_responses": ["request_key"]}[table]
    stamp = {"attractions": "source_modified_at", "attraction_images": "updated_at", "kto_responses": "fetched_at"}.get(table)
    result = {}
    allowed = set(TABLES[table]) | ({"content_id"} if table == "attraction_images" else set())
    for source in rows:
        row = {key: (None if isinstance(value,str) and not value.strip() else value) for key, value in source.items() if key in allowed}
        if any(row.get(key) in (None, "") for key in keys):
            raise ValueError(f"{table}: 식별자가 없습니다")
        if table == "kto_responses" and ("servicekey" in row["request_key"].lower() or not row["request_key"].startswith("/B551011/")):
            raise ValueError("인증키가 포함되었거나 허용되지 않는 원천 응답입니다")
        key = tuple(row[k] for k in keys)
        old = result.get(key)
        if old is None:
            result[key] = row
        elif not stamp or newer(row.get("source_modified_at") or row.get(stamp), old.get("source_modified_at") or old.get(stamp)):
            result[key] = {**old, **{k: v for k, v in row.items() if v not in (None, "")}}
    return list(result.values())

def newer(incoming, existing):
    from datetime import datetime, timezone
    def parse(value):
        return datetime.fromisoformat(value.replace("Z", "+00:00")) if value else datetime.min.replace(tzinfo=timezone.utc)
    return parse(incoming) >= parse(existing)

def export_seed(args):
    # 하나의 읽기 전용 스냅샷에서 관광 데이터만 추출한다.
    queries = []
    for table, columns in TABLES.items():
        if table == "attraction_images":
            selection = "SELECT a.content_id," + ",".join("i." + c for c in columns) + " FROM tourism_source.attraction_images i JOIN tourism_source.attractions a ON a.no=i.attraction_no WHERE i.is_active"
        else:
            selection = "SELECT " + ",".join(columns) + " FROM tourism_source." + table
            if table == "kto_responses": selection += " WHERE payload IS NOT NULL"
        queries.append("SELECT coalesce(jsonb_agg(to_jsonb(t)), '[]'::jsonb) FROM (" + selection + ") t;")
    lines = psql(args, "BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;\n" + "\n".join(queries) + "\nCOMMIT;").splitlines()
    data = {"version": 1, "tables": {table: deduplicate(table, json.loads(line)) for table, line in zip(TABLES, lines)}}
    if len(lines) != len(TABLES): raise RuntimeError("불완전한 export 응답")
    args.file.parent.mkdir(parents=True, exist_ok=True)
    args.file.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print({table: len(rows) for table, rows in data["tables"].items()})

def import_sql(data):
    if data.get("version") != 1 or set(data.get("tables", {})) != set(TABLES): raise ValueError("지원하지 않는 seed 형식")
    sql = ["BEGIN;", "SELECT pg_advisory_xact_lock(520052);", "LOCK TABLE tourism_source.contenttypes, tourism_source.attractions, tourism_source.attraction_images, tourism_source.kto_responses IN SHARE ROW EXCLUSIVE MODE;"]
    for table, columns in TABLES.items():
        rows = deduplicate(table, data["tables"][table])
        if not rows: continue
        if table == "attraction_images":
            for row in rows:
                content_id = int(row.pop("content_id"))
                value = json_sql(row)
                sql.append(f"""WITH source AS (SELECT r.*, a.no AS parent FROM jsonb_populate_record(null::tourism_source.attraction_images,{value}) r CROSS JOIN tourism_source.attractions a WHERE a.content_id={content_id}),
                updated AS (UPDATE tourism_source.attraction_images i SET original_url=coalesce(s.original_url,i.original_url),width=coalesce(s.width,i.width),height=coalesce(s.height,i.height),updated_at=coalesce(s.updated_at,i.updated_at)
                FROM source s WHERE i.attraction_no=s.parent AND i.public_url=s.public_url AND (s.updated_at>=i.updated_at OR i.updated_at IS NULL) RETURNING i.id)
                INSERT INTO tourism_source.attraction_images(id,attraction_no,source_provider,source_type,original_url,public_url,display_order,width,height,checksum_sha256,is_active,created_at,updated_at)
                SELECT gen_random_uuid(),s.parent,coalesce(s.source_provider,'KTO'),s.source_type,s.original_url,s.public_url,coalesce(s.display_order,999),s.width,s.height,s.checksum_sha256,true,coalesce(s.created_at,now()),coalesce(s.updated_at,now()) FROM source s
                WHERE NOT EXISTS(SELECT 1 FROM tourism_source.attraction_images i WHERE i.attraction_no=s.parent AND i.public_url=s.public_url);""")
            continue
        column_list = ",".join(columns)
        key = {"contenttypes": "content_type_id", "attractions": "content_id", "kto_responses": "request_key"}[table]
        assignments = ",".join(f"{c}=coalesce(excluded.{c},{table}.{c})" for c in columns if c != key)
        predicate = ""
        if table == "attractions":
            predicate = " WHERE attractions.source_modified_at IS NULL OR excluded.source_modified_at>=attractions.source_modified_at"
        elif table == "kto_responses":
            rows = [row for row in rows if row.get("payload") is not None]
            predicate = " WHERE kto_responses.fetched_at IS NULL OR excluded.source_modified_at>kto_responses.source_modified_at OR (excluded.source_modified_at IS NOT NULL AND kto_responses.source_modified_at IS NULL) OR (excluded.source_modified_at IS NOT DISTINCT FROM kto_responses.source_modified_at AND excluded.fetched_at>kto_responses.fetched_at)"
        value = json_sql(rows)
        sql.append(f"INSERT INTO tourism_source.{table}({column_list}) SELECT {column_list} FROM jsonb_populate_recordset(null::tourism_source.{table},{value}) ON CONFLICT({key}) DO UPDATE SET {assignments}{predicate};")
    sql.append("COMMIT;")
    return "\n".join(sql)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["export", "import", "refresh"])
    parser.add_argument("file", type=Path)
    parser.add_argument("--container", default="soomgil-postgres-1")
    parser.add_argument("--database", default="soomgil")
    parser.add_argument("--user", default="soomgil")
    parser.add_argument("--dry-run", action="store_true")
    args=parser.parse_args()
    if args.action == "refresh":
        content_id = int(str(args.file))
        key = f"/B551011/KorService2/detailCommon2?contentId={content_id}"
        sql = f"""BEGIN;
        INSERT INTO tourism_source.kto_responses(request_key,refresh_requested) VALUES ('{key}',true)
        ON CONFLICT(request_key) DO UPDATE SET refresh_requested=true;
        UPDATE tourism_source.kto_responses SET refresh_requested=true WHERE request_key ~ '[?&]contentId={content_id}(&|$)';
        COMMIT;"""
        if args.dry_run: print("수동 갱신 대상:", content_id)
        else: psql(args,sql); print("다음 상세 조회 때 재수집합니다. 현재 데이터는 유지합니다.")
    elif args.action == "export": export_seed(args)
    else:
        data=json.loads(args.file.read_text(encoding="utf-8-sig"))
        sql=import_sql(data)
        if args.dry_run: print("seed 검증 완료. DB에 적용하지 않았습니다.")
        else: psql(args,sql); print("seed 가져오기 완료")

if __name__ == "__main__": main()
