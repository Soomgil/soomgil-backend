import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

import place_tagging as target


def place(content_id, area_code, title="장소"):
    return {"content_id": str(content_id), "area_code": str(area_code), "title": title,
            "category": "관광지", "address": "주소", "overview": "소개", "source_hash": "h"}


class PartitionTest(unittest.TestCase):
    def test_three_parts_balance_by_place_count_and_keep_whole_areas(self):
        places = [place(i, 1) for i in range(100)] + [place(200 + i, 2) for i in range(60)] \
            + [place(300 + i, 3) for i in range(50)] + [place(400 + i, 4) for i in range(40)]

        parts = target.partition_by_area(places, part_count=3)

        self.assertEqual(["A", "B", "C"], sorted(parts))
        self.assertEqual({"1"}, set(parts["A"]))                 # 가장 큰 지역은 혼자 한 파트
        self.assertEqual({"2"}, set(parts["B"]))                 # 60
        self.assertEqual({"3", "4"}, set(parts["C"]))            # 50+40 (가장 적게 쌓인 파트에 붙인다)
        assigned = [code for codes in parts.values() for code in codes]
        self.assertEqual(sorted(assigned), sorted(set(assigned)))  # 지역 중복 없음

    def test_partition_is_deterministic_and_excludes_jeju_by_default(self):
        places = [place(1, 39), place(2, 1), place(3, 6)]

        first = target.partition_by_area(places, part_count=3)
        second = target.partition_by_area(list(reversed(places)), part_count=3)

        self.assertEqual(first, second)
        self.assertNotIn("39", [code for codes in first.values() for code in codes])

    def test_part_label_accepts_letters_and_numbers(self):
        self.assertEqual("A", target.normalize_part("a"))
        self.assertEqual("B", target.normalize_part("2"))
        self.assertEqual("C", target.normalize_part(" c "))
        with self.assertRaises(ValueError):
            target.normalize_part("D")


class KeyPoolTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.state = Path(self.directory.name) / "quota.json"
        self.now = datetime(2026, 9, 16, 10, 0, tzinfo=target.KST)

    def tearDown(self):
        self.directory.cleanup()

    def test_part_picks_its_primary_key_then_falls_back_when_exhausted(self):
        pool = target.KeyPool(["key-one", "key-two"], part="B", state_path=self.state, clock=lambda: self.now)

        self.assertEqual("key-two", pool.current())          # B는 두 번째 키가 기본
        pool.mark_exhausted("key-two")
        self.assertEqual("key-one", pool.current())          # 소진되면 다른 키로
        pool.mark_exhausted("key-one")
        self.assertIsNone(pool.current())                    # 둘 다 소진

    def test_exhausted_keys_reset_after_kst_midnight(self):
        pool = target.KeyPool(["key-one"], part="A", state_path=self.state, clock=lambda: self.now)
        pool.mark_exhausted("key-one")
        self.assertIsNone(pool.current())
        self.assertEqual(timedelta(hours=14), pool.wait_until_reset())

        self.now = self.now + timedelta(hours=14, minutes=1)
        self.assertEqual("key-one", pool.current())

    def test_request_counts_persist_per_key_and_day(self):
        pool = target.KeyPool(["key-one"], part="A", state_path=self.state, clock=lambda: self.now)
        pool.record_request("key-one")
        pool.record_request("key-one")

        reloaded = target.KeyPool(["key-one"], part="A", state_path=self.state, clock=lambda: self.now)
        self.assertEqual(2, reloaded.requests_today("key-one"))
        self.assertNotIn("key-one", self.state.read_text(encoding="utf-8"))  # 키 원문은 저장하지 않음

    def test_daily_limit_marks_key_exhausted(self):
        pool = target.KeyPool(["key-one", "key-two"], part="A", state_path=self.state,
                              clock=lambda: self.now, daily_limit=2)
        pool.record_request("key-one")
        pool.record_request("key-one")

        self.assertEqual("key-two", pool.current())


class QuotaErrorTest(unittest.TestCase):
    def test_detects_quota_exhaustion_from_status_and_message(self):
        self.assertTrue(target.is_quota_error(429, ""))
        self.assertTrue(target.is_quota_error(403, '{"error":{"status":"RESOURCE_EXHAUSTED"}}'))
        self.assertTrue(target.is_quota_error(400, "daily quota exceeded"))
        self.assertFalse(target.is_quota_error(400, "invalid argument"))
        self.assertFalse(target.is_quota_error(500, "internal"))


class ResumeTest(unittest.TestCase):
    def test_pending_excludes_already_tagged_places(self):
        with tempfile.TemporaryDirectory() as directory:
            tagged = Path(directory) / "tagged.jsonl"
            tagged.write_text(json.dumps({**place(1, 1), "tags": []}) + "\n", encoding="utf-8")

            pending, completed = target.split_pending([place(1, 1), place(2, 1)], tagged)

        self.assertEqual(["2"], [row["content_id"] for row in pending])
        self.assertEqual(["1"], sorted(completed))

    def test_extract_keeps_area_code_for_all_regions(self):
        def row(content_id, area_code):
            values = ["1", str(content_id), "이름", "12", str(area_code), "4", "img", "img",
                      "6", "33.0", "126.0", "", "주소", "", "", "소개"]
            return "(" + ",".join("'" + value + "'" for value in values) + ")"

        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "dump.sql"
            destination = Path(directory) / "places.jsonl"
            source.write_text("INSERT INTO `attractions` VALUES " + row(1, 1) + "," + row(2, 39) + ";", encoding="utf-8")

            places = target.extract_all_places(source, destination)

        self.assertEqual([("1", "1"), ("2", "39")], [(p["content_id"], p["area_code"]) for p in places])


if __name__ == "__main__":
    unittest.main()
