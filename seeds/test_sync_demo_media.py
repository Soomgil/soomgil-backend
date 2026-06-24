import unittest
from unittest.mock import patch

import sync_demo_media


class ResolveSourceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.pool = [
            {
                "url": f"https://images.example/{index}.jpg?w=1400&h=900",
                "mime_type": "image/jpeg",
                "source_page": f"https://photos.example/{index}",
                "license": "test",
                "artist": "test",
            }
            for index in range(3)
        ]

    def test_uses_an_unused_source_when_one_is_available(self) -> None:
        asset = {"kind": "record", "object_key": "demo/records/1/cover.jpg", "search_term": "서울", "variant": 0}
        used = {self.pool[0]["source_page"], self.pool[1]["source_page"]}

        with patch.object(sync_demo_media, "unsplash_photo_pool", return_value=self.pool):
            source = sync_demo_media.resolve_source(asset, used)

        self.assertEqual(self.pool[2]["source_page"], source["source_page"])

    def test_requests_a_portrait_crop_for_portrait_objects(self) -> None:
        asset = {"kind": "record", "object_key": "demo/records/1/portrait-v2.jpg", "search_term": "서울", "variant": 0}

        with patch.object(sync_demo_media, "unsplash_photo_pool", return_value=self.pool):
            source = sync_demo_media.resolve_source(asset)

        self.assertIn("w=900&h=1350", source["url"])


if __name__ == "__main__":
    unittest.main()
