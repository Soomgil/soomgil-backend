package com.soomgil.place.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.List;

/**
 * 지도 viewport 안의 장소 후보를 조회하는 query.
 *
 * @param bbox 지도 viewport 문자열
 * @param category 관광지 분류
 * @param limit 최대 후보 수
 */
public record PlaceViewportCandidateQuery(
	String bbox,
	String category,
	int limit
) implements Query<List<PlaceViewportCandidate>> {
}
