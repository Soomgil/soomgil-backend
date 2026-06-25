package com.soomgil.place.application.port;

import java.util.List;
import java.util.Optional;

/**
 * 관광공사 원격 API에서 스와이프 장소 후보를 조회하는 port.
 */
public interface TourismPlaceFeedClient {

	TourismPlaceFeedResult fetch(TourismPlaceFeedRequest request);

	default List<TourismPlaceFeedItem> fetchLive(TourismPlaceLiveSearchRequest request) {
		return List.of();
	}

	default Optional<TourismPlaceFeedItem> fetchOne(String externalPlaceId) {
		return Optional.empty();
	}

	/**
	 * KTO detailIntro 응답에서 접근성/운영시간/주차 등의 raw 텍스트를 조회한다.
	 * contentTypeId별로 KTO가 제공하는 필드명이 다르지만, 구현체가 이를 통일된
	 * {@link PlaceIntroRaw} 형태로 정규화하여 반환한다.
	 *
	 * @param contentId      KTO 콘텐츠 ID (externalPlaceId)
	 * @param contentTypeId  KTO 콘텐츠 타입 ID (12=관광지, 14=문화시설, ...)
	 * @return raw intro 데이터. 호출에 실패하거나 데이터가 없으면 {@link PlaceIntroRaw#empty()}
	 */
	PlaceIntroRaw fetchIntro(String contentId, String contentTypeId);
}
