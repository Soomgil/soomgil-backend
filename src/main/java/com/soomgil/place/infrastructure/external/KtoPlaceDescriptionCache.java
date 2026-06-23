package com.soomgil.place.infrastructure.external;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * KTO 공통 상세 설명을 원천 수정 시각 단위로 캐시한다.
 */
@Component
public class KtoPlaceDescriptionCache {

	static final String CACHE_NAME = "ktoPlaceDescription";

	private final Cache cache;

	public KtoPlaceDescriptionCache(CacheManager cacheManager) {
		this.cache = cacheManager.getCache(CACHE_NAME);
		if (this.cache == null) {
			throw new IllegalStateException("Missing cache: " + CACHE_NAME);
		}
	}

	/**
	 * 같은 contentId와 KTO 원천 수정 시각으로 저장된 설명을 조회한다.
	 */
	public Optional<String> find(TourismPlaceFeedItem place) {
		return Optional.ofNullable(cache.get(key(place), String.class));
	}

	/**
	 * 비어 있지 않은 설명만 현재 원천 수정 시각의 캐시로 저장한다.
	 */
	public void put(TourismPlaceFeedItem place, String description) {
		if (description != null && !description.isBlank()) {
			cache.put(key(place), description);
		}
	}

	private String key(TourismPlaceFeedItem place) {
		String modifiedAt = place.sourceModifiedAt() == null
			? "unknown"
			: place.sourceModifiedAt().toInstant().toString();
		return place.externalPlaceId() + ":" + modifiedAt;
	}
}
