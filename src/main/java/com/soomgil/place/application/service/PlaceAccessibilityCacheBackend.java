package com.soomgil.place.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.infrastructure.persistence.repository.PlaceAccessibilityOverrideRepository;
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
	private final PlaceAccessibilityOverrideRepository overrideRepository;

	public PlaceAccessibilityCacheBackend(
		TourismPlaceFeedClient client,
		PlaceAccessibilityNormalizer normalizer,
		PlaceAccessibilityOverrideRepository overrideRepository
	) {
		this.client = client;
		this.normalizer = normalizer;
		this.overrideRepository = overrideRepository;
	}

	@Cacheable(value = "placeAccessibility", key = "'v2:' + #provider + ':' + #externalPlaceId")
	public PlaceAccessibilityInfo load(String provider, String externalPlaceId, String contentTypeId) {
		return overrideRepository.find(provider, externalPlaceId)
			.orElseGet(() -> loadFromKto(externalPlaceId, contentTypeId));
	}

	private PlaceAccessibilityInfo loadFromKto(String externalPlaceId, String contentTypeId) {
		return normalizer.normalize(client.fetchIntro(externalPlaceId, contentTypeId));
	}
}
