package com.soomgil.place.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 실제 KTO detailIntro 호출 + 정규화 + 캐시 저장을 담당하는 백엔드.
 * {@link PlaceAccessibilityCacheService}가 동일 bean 내에서 self-invocation으로
 * {@code @Cacheable}을 우회하는 문제를 피하기 위해 분리했다.
 */
@Service
public class PlaceAccessibilityCacheBackend {

	private final TourismPlaceFeedClient client;
	private final PlaceAccessibilityNormalizer normalizer;

	public PlaceAccessibilityCacheBackend(
		TourismPlaceFeedClient client,
		PlaceAccessibilityNormalizer normalizer
	) {
		this.client = client;
		this.normalizer = normalizer;
	}

	@Cacheable(value = "placeAccessibility", key = "#provider + ':' + #externalPlaceId")
	public PlaceAccessibilityInfo load(String provider, String externalPlaceId, String contentTypeId) {
		return normalizer.normalize(client.fetchIntro(externalPlaceId, contentTypeId));
	}
}
