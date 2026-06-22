package com.soomgil.search.api.dto;

import com.soomgil.community.api.dto.CommunityPostSummary;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.trip.api.dto.TripSummary;
import java.util.List;

/**
 * 통합 검색 응답.
 *
 * <p>4개 도메인(trips/places/posts/users)의 검색 결과를 한 번에 반환한다.
 * 각 섹션은 최대 {@code size}개(기본 4)씩 포함한다.
 * 프론트는 "자세히 보기" 진입 시 각 도메인의 개별 검색 엔드포인트로 페이지네이션한다.
 */
public record UnifiedSearchResponse(
	String query,
	List<TripSummary> trips,
	List<PlaceSummary> places,
	List<CommunityPostSummary> posts,
	List<UserSearchResult> users
) {
}
