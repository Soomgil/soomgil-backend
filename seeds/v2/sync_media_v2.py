#!/usr/bin/env python3
"""데모 v2 미디어를 S3 호환 버킷에 올린다 (AWS 배포용).

로컬 생성기가 남긴 media-manifest-v2.csv(object_key ↔ KTO 원본 URL)를 읽어 각 이미지를 내려받아
같은 object_key 로 업로드한다. DB 덤프의 media.media_files.object_key 와 정확히 일치해야 이미지가 보인다.

사용: python3 seeds/v2/sync_media_v2.py [--env ../.env.aws] [--dry-run]
필요 값(.env 또는 환경변수): S3_ENDPOINT, S3_REGION, S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY
"""
from __future__ import annotations
import argparse, csv, os, sys, time
from pathlib import Path
from urllib.request import Request, urlopen

try:
    import boto3
    from botocore.exceptions import ClientError
except ImportError:
    raise SystemExit("pip install boto3  (or: pip install -r seeds/requirements.txt)")

HERE = Path(__file__).resolve().parent
MANIFEST = HERE / "media-manifest-v2.csv"
UA = "Mozilla/5.0 SoomgilDemoSeeder/2.0"

def load_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8-sig").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1); values[k.strip()] = v.strip().strip('"').strip("'")
    return values

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--env", default=str(HERE / ".." / ".." / ".." / ".env.aws"), help=".env file with S3_* values")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    cfg = {**load_env(Path(args.env)), **os.environ}
    need = ["S3_ENDPOINT", "S3_REGION", "S3_BUCKET", "S3_ACCESS_KEY", "S3_SECRET_KEY"]
    missing = [k for k in need if not cfg.get(k)]
    if missing: raise SystemExit(f"missing settings: {', '.join(missing)}")
    rows = list(csv.DictReader(MANIFEST.open(encoding="utf-8")))
    print(f"manifest: {len(rows)} objects → s3://{cfg['S3_BUCKET']} ({cfg['S3_ENDPOINT']})")
    s3 = boto3.client("s3", endpoint_url=cfg["S3_ENDPOINT"], region_name=cfg["S3_REGION"],
                      aws_access_key_id=cfg["S3_ACCESS_KEY"], aws_secret_access_key=cfg["S3_SECRET_KEY"])
    done = skipped = failed = 0
    for i, row in enumerate(rows, 1):
        key, url, mime = row["object_key"], row["source_url"], row["mime"]
        try:
            try:
                s3.head_object(Bucket=cfg["S3_BUCKET"], Key=key); skipped += 1
                print(f"[{i}/{len(rows)}] exists  {key}"); continue
            except ClientError as e:
                if e.response["Error"]["Code"] not in ("404", "NoSuchKey", "NotFound"): raise
            if args.dry_run:
                print(f"[{i}/{len(rows)}] would upload {key} ← {url}"); continue
            for attempt in range(3):
                try:
                    with urlopen(Request(url.replace("http://", "https://", 1), headers={"User-Agent": UA}), timeout=30) as r:
                        data = r.read()
                    break
                except Exception:
                    if attempt == 2: raise
                    time.sleep(1.5 * (attempt + 1))
            s3.put_object(Bucket=cfg["S3_BUCKET"], Key=key, Body=data, ContentType=mime, CacheControl="public, max-age=31536000")
            s3.head_object(Bucket=cfg["S3_BUCKET"], Key=key)
            done += 1; print(f"[{i}/{len(rows)}] uploaded {key} ({len(data)//1024} KB)")
        except Exception as e:  # noqa: BLE001
            failed += 1; print(f"[{i}/{len(rows)}] FAILED  {key}: {e}", file=sys.stderr)
    print(f"done: uploaded={done} existing={skipped} failed={failed}")
    return 1 if failed else 0

if __name__ == "__main__":
    raise SystemExit(main())
