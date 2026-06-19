package com.soomgil.place.application.query.dto;

/**
 * 관광 원천 저장소에 전달하는 장소 검색 조건.
 *
 * @param q 검색어
 * @param bbox 지도 viewport 문자열
 * @param legalRegionCode 지역 코드
 * @param category 관광지 분류
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record PlaceSearchCriteria(
	String q,
	String bbox,
	String legalRegionCode,
	String category,
	int page,
	int size
) {
}
