package com.soomgil.place.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.place.api.dto.PagedPlaceSummary;

/**
 * 관광지 검색 조건을 표현하는 query.
 *
 * @param q 검색어
 * @param bbox 지도 viewport 문자열
 * @param legalRegionCode 지역 코드
 * @param category 관광지 분류
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record PlaceSearchQuery(
	String q,
	String bbox,
	String legalRegionCode,
	String category,
	int page,
	int size
) implements Query<PagedPlaceSummary> {
}
