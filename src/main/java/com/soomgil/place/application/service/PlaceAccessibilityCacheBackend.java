package com.soomgil.place.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.infrastructure.persistence.repository.PlaceAccessibilityOverrideRepository;
import org.springframework.stereotype.Service;

/**
 * 장소별 운영 정보 재정의를 우선 조회하고 KTO 응답을 정규화한다.
 * 성공 응답의 영속 캐시와 실패 재시도 제한은 KTO 클라이언트에서 처리한다.
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

	public PlaceAccessibilityInfo load(String provider, String externalPlaceId, String contentTypeId) {
		return overrideRepository.find(provider, externalPlaceId)
			.orElseGet(() -> loadFromKto(externalPlaceId, contentTypeId));
	}

	private PlaceAccessibilityInfo loadFromKto(String externalPlaceId, String contentTypeId) {
		return normalizer.normalize(client.fetchIntro(externalPlaceId, contentTypeId));
	}
}
