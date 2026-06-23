package com.soomgil.place.infrastructure.external;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * KTO 상세 이미지 URL을 원천 수정 시각 단위로 캐시한다.
 */
@Component
public class KtoPlacePhotoCache {

	static final String CACHE_NAME = "ktoPlacePhotos";
	private static final String SEPARATOR = "\u001e";

	private final Cache cache;

	public KtoPlacePhotoCache(CacheManager cacheManager) {
		this.cache = cacheManager.getCache(CACHE_NAME);
		if (this.cache == null) {
			throw new IllegalStateException("Missing cache: " + CACHE_NAME);
		}
	}

	/**
	 * 같은 contentId와 KTO 원천 수정 시각으로 저장된 상세 사진을 조회한다.
	 */
	public Optional<List<String>> find(TourismPlaceFeedItem place) {
		String value = cache.get(key(place), String.class);
		if (value == null) {
			return Optional.empty();
		}
		if (value.isEmpty()) {
			return Optional.of(List.of());
		}
		return Optional.of(Arrays.asList(value.split(SEPARATOR, -1)));
	}

	/**
	 * 상세 사진 URL 목록을 현재 원천 수정 시각의 캐시로 저장한다.
	 */
	public void put(TourismPlaceFeedItem place, List<String> photos) {
		cache.put(key(place), String.join(SEPARATOR, photos));
	}

	private String key(TourismPlaceFeedItem place) {
		String modifiedAt = place.sourceModifiedAt() == null
			? "unknown"
			: place.sourceModifiedAt().toInstant().toString();
		return place.externalPlaceId() + ":" + modifiedAt;
	}
}
