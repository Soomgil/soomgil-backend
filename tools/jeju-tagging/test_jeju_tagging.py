import tempfile
import unittest
from pathlib import Path

import jeju_tagging as target


class JejuTaggingTest(unittest.TestCase):
    def test_parse_mysql_values_handles_escapes_null_and_newline(self):
        sql = " (1,'제주\\'바다',NULL,'두 줄\\n설명'),(2,'오''름',39,'설명');tail"

        rows, end = target.parse_mysql_values(sql, 0)

        self.assertEqual([["1", "제주'바다", None, "두 줄\n설명"], ["2", "오'름", "39", "설명"]], rows)
        self.assertEqual("tail", sql[end:])

    def test_extract_places_uses_only_jeju_and_excludes_images(self):
        def row(content_id, area_code, title):
            values = [
                "1", str(content_id), title, "12", str(area_code), "4", "secret-image-1", "secret-image-2",
                "6", "33.0", "126.0", "", "주소", "", "", "소개",
            ]
            return "(" + ",".join("'" + value + "'" for value in values) + ")"

        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "dump.sql"
            destination = Path(directory) / "places.jsonl"
            source.write_text(
                "INSERT INTO `attractions` VALUES " + row(100, 39, "제주") + "," + row(200, 1, "서울") + ";",
                encoding="utf-8",
            )

            places = target.extract_places(source, destination)

        self.assertEqual(1, len(places))
        self.assertEqual("100", places[0]["content_id"])
        self.assertNotIn("secret-image", str(places[0]))

    def test_selection_matches_backend_default_policy(self):
        row = {
            "tags": [
                {"code": "coast", "confidence": 0.9, "weight": 0.8, "rationale": "해안"},
                {"code": "quiet", "confidence": 0.5, "weight": 1.0, "rationale": "조용함"},
            ]
        }

        decisions, selected = target.selected_tags(row)

        self.assertEqual(["coast"], [item["code"] for item in selected])
        self.assertEqual("REJECTED_LOW_CONFIDENCE", next(item for item in decisions if item["code"] == "quiet")["status"])

    def test_parse_json_parts_skips_thought_and_code_fence(self):
        parts = [
            {"text": ""},
            {"text": "```json\n[{\"content_id\":\"1\",\"tags\":[]}]\n```"},
        ]

        parsed = target.parse_json_parts(parts)

        self.assertEqual("1", parsed[0]["content_id"])

    def test_parse_sse_response_repairs_truncated_structured_output_prefix(self):
        events = [
            'data: {"candidates":[{"content":{"parts":[{"text":"_id\\\": \\\"125445\\\", \\\"tags\\\": ["}]}}]}',
            'data: {"candidates":[{"content":{"parts":[{"text":"]}]"}]}}]}',
        ]

        parsed = target.parse_sse_response("\n\n".join(events), ["125445"])

        self.assertEqual([{"content_id": "125445", "tags": []}], parsed)


if __name__ == "__main__":
    unittest.main()
