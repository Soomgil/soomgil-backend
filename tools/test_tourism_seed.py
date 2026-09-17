import unittest
from tourism_seed import deduplicate, import_sql

class SeedTests(unittest.TestCase):
    def test_duplicates_keep_latest_source_and_preserve_nonempty_fields(self):
        rows=[{"content_id":1,"title":"old","overview":"설명","source_modified_at":"2026-01-01T00:00:00Z"},
              {"content_id":1,"title":"new","overview":"","source_modified_at":"2026-02-01T00:00:00+00:00"},
              {"content_id":1,"title":"older","source_modified_at":"2025-12-01T00:00:00Z"}]
        result=deduplicate("attractions",rows)
        self.assertEqual(len(result),1)
        self.assertEqual(result[0]["title"],"new")
        self.assertEqual(result[0]["overview"],"설명")
    def test_image_identity_uses_content_id_not_local_primary_key(self):
        rows=[{"content_id":1,"public_url":"https://example.org/a.jpg","attraction_no":900}]*2
        self.assertEqual(deduplicate("attraction_images",rows),[{"content_id":1,"public_url":"https://example.org/a.jpg"}])
    def test_credentials_are_rejected(self):
        with self.assertRaises(ValueError): deduplicate("kto_responses",[{"request_key":"/B551011/KorService2/detailCommon2?serviceKey=secret"}])
    def test_seed_is_transactional_and_values_are_not_sql(self):
        data={"version":1,"tables":{"contenttypes":[],"attractions":[{"content_id":1,"title":"'); DROP TABLE users;--"}],"attraction_images":[],"kto_responses":[]}}
        sql=import_sql(data)
        self.assertTrue(sql.startswith("BEGIN;"))
        self.assertTrue(sql.endswith("COMMIT;"))
        self.assertNotIn("DROP TABLE users",sql)

if __name__=="__main__": unittest.main()
