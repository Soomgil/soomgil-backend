#!/usr/bin/env python3
"""Download realistic demo images and upload them to the configured private S3 bucket."""

from __future__ import annotations

import argparse
import concurrent.futures
import csv
import hashlib
import io
import json
import mimetypes
import os
from pathlib import Path
import subprocess
import sys
import threading
import time
from typing import Any, Optional
from urllib.parse import urlencode
from urllib.error import HTTPError
from urllib.request import Request, urlopen

try:
    import boto3
except ImportError as exc:
    raise SystemExit("Install demo dependencies first: pip install -r seeds/requirements.txt") from exc


ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env"
ORCHESTRATION_ENV_FILE = ROOT.parent / ".env"
MANIFEST_FILE = ROOT / "seeds" / "generated" / "demo-media-manifest.csv"
USER_AGENT = "SoomgilDemoSeeder/1.0 (https://github.com/Soomgil/soomgil-backend)"
MAX_DOWNLOAD_BYTES = 12 * 1024 * 1024
UNSPLASH_QUERIES = [
    "korea travel", "seoul travel", "south korea landscape", "korean palace",
    "korea city night", "korea nature", "korea street",
]

_download_cache: dict[str, tuple[bytes, str]] = {}
_download_lock = threading.Lock()
_unsplash_pool: Optional[list[dict[str, str]]] = None


def load_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8-sig").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def required(config: dict[str, str], key: str) -> str:
    value = config.get(key) or os.environ.get(key)
    if not value:
        raise SystemExit(f"Missing required setting: {key}")
    return value


def query_assets(container: str, db_user: str, db_name: str) -> list[dict[str, Any]]:
    sql = r"""
COPY (
  WITH assets AS (
    SELECT 'profile' kind,
           'demo/profiles/' || p.user_id || '.png' object_key,
           p.display_name search_term,
           0 variant
    FROM auth.user_profiles p
    WHERE p.profile_image_url LIKE
      'https://daobk0bynum21.cloudfront.net/demo/profiles/%'

    UNION ALL

    SELECT 'place', 'demo/places/' || a.content_id || '/cover.jpg', a.title || ' 대한민국 여행', 0
    FROM tourism_source.attractions a
    WHERE a.content_id BETWEEN 10001 AND 10040 OR a.content_id BETWEEN 20001 AND 20028

    UNION ALL

    SELECT 'place',
           replace(i.thumbnail_url, 'https://daobk0bynum21.cloudfront.net/', ''),
           min(i.place_name) || ' 대한민국 여행',
           abs(hashtext(i.thumbnail_url)) % 5
    FROM itinerary.itinerary_items i
    WHERE i.thumbnail_url LIKE
      'https://daobk0bynum21.cloudfront.net/demo/legacy-places/%'
    GROUP BY i.thumbnail_url

    UNION ALL

    SELECT 'place', 'demo/places/' || a.content_id || '/detail.jpg', a.title || ' 대한민국 여행', 1
    FROM tourism_source.attractions a
    WHERE a.content_id BETWEEN 10001 AND 10040 OR a.content_id BETWEEN 20001 AND 20028

    UNION ALL

    SELECT 'record', m.object_key, COALESCE(r.location_name, '대한민국 여행'),
           abs(hashtext(m.object_key)) % 5
    FROM media.media_files m
    JOIN record.trip_record_entries r ON m.linked_resource_type = 'TRIP_RECORD'
      AND m.linked_resource_id = r.id
    WHERE m.object_key LIKE 'demo/%'

    UNION ALL

    SELECT 'record', m.object_key,
           COALESCE(r.location_name, t.display_destination, '대한민국 여행'),
           abs(hashtext(m.object_key)) % 5
    FROM media.media_files m
    JOIN trip.trips t ON m.linked_resource_type = 'trip.trips'
      AND m.linked_resource_id = t.id
    LEFT JOIN record.trip_record_media rm ON rm.media_file_id = m.id
    LEFT JOIN record.trip_record_entries r ON r.id = rm.record_entry_id
    WHERE m.object_key LIKE 'demo/trips/%'

    UNION ALL

    SELECT 'community', m.object_key,
           COALESCE(first_item.place_name, t.display_destination, '대한민국 여행'),
           abs(hashtext(m.object_key)) % 5
    FROM media.media_files m
    JOIN community.posts p ON m.linked_resource_type = 'COMMUNITY_POST'
      AND m.linked_resource_id = p.id
    LEFT JOIN trip.trips t ON t.id = p.source_trip_id
    LEFT JOIN LATERAL (
      SELECT i.place_name
      FROM itinerary.itinerary_items i
      JOIN itinerary.itinerary_days d ON d.id = i.itinerary_day_id
      WHERE i.trip_id = p.source_trip_id AND i.deleted_at IS NULL
      ORDER BY d.sort_order, i.sort_order
      LIMIT 1
    ) first_item ON true
    WHERE m.object_key LIKE 'demo/%'
  )
  SELECT json_build_object(
    'kind', kind,
    'object_key', object_key,
    'search_term', search_term,
    'variant', variant
  )::text
  FROM assets
  ORDER BY kind, object_key
) TO STDOUT;
"""
    result = subprocess.run(
        ["docker", "exec", "-i", container, "psql", "-U", db_user, "-d", db_name,
         "-v", "ON_ERROR_STOP=1", "-q", "-t", "-A"],
        input=sql,
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(f"Could not query demo assets:\n{result.stderr.strip()}")
    rows = [json.loads(line) for line in result.stdout.splitlines() if line.strip().startswith("{")]
    unique: dict[str, dict[str, Any]] = {row["object_key"]: row for row in rows}
    return list(unique.values())


def unsplash_photo_pool() -> list[dict[str, str]]:
    global _unsplash_pool
    if _unsplash_pool is not None:
        return _unsplash_pool
    photos: dict[str, dict[str, str]] = {}
    for query in UNSPLASH_QUERIES:
        url = "https://unsplash.com/napi/search/photos?" + urlencode({
            "query": query,
            "per_page": "30",
            "page": "1",
        })
        request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
        try:
            with urlopen(request, timeout=30) as response:
                payload = json.load(response)
        except Exception:
            continue
        for item in payload.get("results", []):
            photo_id = item.get("id")
            raw_url = (item.get("urls") or {}).get("raw")
            if not photo_id or not raw_url:
                continue
            separator = "&" if "?" in raw_url else "?"
            photos[photo_id] = {
                "url": raw_url + separator + "w=1400&h=900&fit=crop&auto=format&q=82",
                "mime_type": "image/jpeg",
                "source_page": f"https://unsplash.com/photos/{photo_id}",
                "license": "Unsplash License",
                "artist": (item.get("user") or {}).get("name", "Unsplash contributor"),
            }
        time.sleep(0.15)
    if not photos:
        photos["fallback"] = {
            "url": "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1400&h=900&fit=crop&q=82",
            "mime_type": "image/jpeg",
            "source_page": "https://unsplash.com/photos/yellow-sunflower-field-during-sunset-Nyvq2juw4_o",
            "license": "Unsplash License",
            "artist": "Unsplash contributor",
        }
    _unsplash_pool = list(photos.values())
    return _unsplash_pool


def resolve_source(asset: dict[str, Any]) -> dict[str, str]:
    if asset["kind"] == "profile":
        seed = hashlib.sha256(asset["object_key"].encode()).hexdigest()[:20]
        return {
            "url": f"https://api.dicebear.com/9.x/notionists/png?seed={seed}&size=512",
            "mime_type": "image/png",
            "source_page": "https://www.dicebear.com/styles/notionists/",
            "license": "CC0 1.0",
            "artist": "DiceBear / Notionists",
        }
    pool = unsplash_photo_pool()
    digest = hashlib.sha256((asset["search_term"] + asset["object_key"]).encode()).digest()
    return pool[int.from_bytes(digest[:4], "big") % len(pool)]


def download(url: str, expected_mime: str) -> tuple[bytes, str]:
    with _download_lock:
        cached = _download_cache.get(url)
    if cached is not None:
        return cached
    request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "image/*"})
    for attempt in range(4):
        try:
            with urlopen(request, timeout=45) as response:
                content_type = response.headers.get_content_type()
                data = response.read(MAX_DOWNLOAD_BYTES + 1)
            break
        except (HTTPError, TimeoutError) as exc:
            retryable = not isinstance(exc, HTTPError) or exc.code in {429, 500, 502, 503, 504}
            if not retryable or attempt == 3:
                raise
            time.sleep(2 ** attempt)
    if len(data) > MAX_DOWNLOAD_BYTES:
        raise ValueError(f"Image exceeds {MAX_DOWNLOAD_BYTES} bytes")
    if not content_type.startswith("image/"):
        content_type = expected_mime or mimetypes.guess_type(url)[0] or "image/jpeg"
    result = (data, content_type)
    with _download_lock:
        _download_cache[url] = result
    return result


def sync_asset(s3: Any, bucket: str, asset: dict[str, Any], dry_run: bool) -> dict[str, Any]:
    source = asset.get("_source") or resolve_source(asset)
    data, content_type = download(source["url"], source["mime_type"])
    sha256 = hashlib.sha256(data).hexdigest()
    if not dry_run:
        s3.put_object(
            Bucket=bucket,
            Key=asset["object_key"],
            Body=io.BytesIO(data),
            ContentType=content_type,
            CacheControl="public, max-age=31536000, immutable",
            Metadata={"sha256": sha256},
        )
        head = s3.head_object(Bucket=bucket, Key=asset["object_key"])
        if head["ContentLength"] != len(data):
            raise ValueError("Uploaded object size does not match source")
        etag = head.get("ETag", "").strip('"')
    else:
        etag = "dry-run"
    return {
        **asset,
        **source,
        "content_type": content_type,
        "byte_size": len(data),
        "sha256": sha256,
        "etag": etag,
    }


def write_manifest(rows: list[dict[str, Any]]) -> None:
    MANIFEST_FILE.parent.mkdir(parents=True, exist_ok=True)
    fields = ["kind", "object_key", "search_term", "variant", "url", "source_page", "license",
              "artist", "content_type", "byte_size", "sha256", "etag"]
    with MANIFEST_FILE.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore", lineterminator="\n")
        writer.writeheader()
        writer.writerows(sorted(rows, key=lambda row: row["object_key"]))


def read_manifest() -> list[dict[str, str]]:
    if not MANIFEST_FILE.exists():
        return []
    with MANIFEST_FILE.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--container", default="soomgil-postgres-1")
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--new-only", action="store_true")
    args = parser.parse_args()

    config = {
        **load_env(ORCHESTRATION_ENV_FILE),
        **load_env(ENV_FILE),
        **os.environ,
    }
    bucket = required(config, "S3_BUCKET")
    region = required(config, "S3_REGION")
    access_key = required(config, "S3_ACCESS_KEY")
    secret_key = required(config, "S3_SECRET_KEY")
    db_user = config.get("DB_USERNAME", "soomgil")
    db_name = config.get("DB_NAME", "soomgil")
    assets = query_assets(args.container, db_user, db_name)
    existing_manifest = read_manifest()
    if args.new_only:
        existing_keys = {row["object_key"] for row in existing_manifest}
        assets = [asset for asset in assets if asset["object_key"] not in existing_keys]
    if args.limit:
        assets = assets[: args.limit]
    if not assets:
        print("No new demo assets to sync.")
        return

    print(f"Resolving image sources for {len(assets)} objects ...")
    for index, asset in enumerate(assets, start=1):
        asset["_source"] = resolve_source(asset)
        if index % 50 == 0 or index == len(assets):
            print(f"  {index}/{len(assets)} sources resolved")

    s3 = boto3.client(
        "s3",
        region_name=region,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        endpoint_url=config.get("S3_ENDPOINT") or None,
    )
    print(f"Syncing {len(assets)} objects to s3://{bucket}/ ...")
    completed: list[dict[str, Any]] = []
    failures: list[tuple[str, str]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {executor.submit(sync_asset, s3, bucket, asset, args.dry_run): asset for asset in assets}
        for index, future in enumerate(concurrent.futures.as_completed(futures), start=1):
            asset = futures[future]
            try:
                completed.append(future.result())
            except Exception as exc:
                failures.append((asset["object_key"], str(exc)))
            if index % 25 == 0 or index == len(assets):
                print(f"  {index}/{len(assets)} processed ({len(failures)} failed)")

    completed_by_key = {row["object_key"]: row for row in existing_manifest}
    completed_by_key.update({row["object_key"]: row for row in completed})
    write_manifest(list(completed_by_key.values()))
    print(f"Manifest: {MANIFEST_FILE}")
    if failures:
        for key, error in failures[:20]:
            print(f"FAILED {key}: {error}", file=sys.stderr)
        raise SystemExit(f"{len(failures)} uploads failed")
    print(f"Verified {len(completed)} objects.")


if __name__ == "__main__":
    main()
