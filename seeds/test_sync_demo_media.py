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

    def test_uses_curated_kto_source_without_unsplash_lookup(self) -> None:
        asset = {
            "kind": "community",
            "object_key": "demo/community/post/cover.jpg",
            "search_term": "성산일출봉",
            "variant": 0,
            "source_url": "http://tong.visitkorea.or.kr/cms/example.jpg",
            "source_page": "http://tong.visitkorea.or.kr/cms/example.jpg",
            "source_license": "한국관광공사 TourAPI 이미지 이용조건",
            "source_artist": "한국관광공사",
        }

        with patch.object(sync_demo_media, "unsplash_photo_pool") as pool:
            source = sync_demo_media.resolve_source(asset)

        pool.assert_not_called()
        self.assertEqual("https://tong.visitkorea.or.kr/cms/example.jpg", source["url"])
        self.assertEqual("한국관광공사", source["artist"])


if __name__ == "__main__":
    unittest.main()
