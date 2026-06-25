package com.soomgil.place.application.port;

/**
 * KTO 원격 API에서 즉시 장소 후보를 조회하기 위한 요청.
 *
 * @param q 검색어
 * @param bbox 지도 viewport 문자열
 * @param legalRegionCode 지역 코드
 * @param category 관광지 분류
 * @param limit 최대 조회 수
 */
public record TourismPlaceLiveSearchRequest(
	String q,
	String bbox,
	String legalRegionCode,
	String category,
	int limit
) {
}
