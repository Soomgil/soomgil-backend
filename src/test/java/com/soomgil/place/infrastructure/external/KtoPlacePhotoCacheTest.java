package com.soomgil.place.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class KtoPlacePhotoCacheTest {

	private final KtoPlacePhotoCache cache = new KtoPlacePhotoCache(
		new ConcurrentMapCacheManager(KtoPlacePhotoCache.CACHE_NAME)
	);

	@Test
	void reusesPhotosOnlyWhileKtoModifiedTimeIsUnchanged() {
		TourismPlaceFeedItem original = place("2026-06-19T17:36:54+09:00");
		TourismPlaceFeedItem updated = place("2026-06-20T09:00:00+09:00");

		cache.put(original, List.of("https://img.example/one.jpg", "https://img.example/two.jpg"));

		assertThat(cache.find(original)).contains(List.of(
			"https://img.example/one.jpg",
			"https://img.example/two.jpg"
		));
		assertThat(cache.find(updated)).isEmpty();
	}

	private TourismPlaceFeedItem place(String modifiedAt) {
		return new TourismPlaceFeedItem(
			"126508", "경복궁", "서울 종로구", 37.5796, 126.9770,
			"https://img.example/main.jpg", "관광지", null,
			List.of("https://img.example/main.jpg"), OffsetDateTime.parse(modifiedAt)
		);
	}
}
