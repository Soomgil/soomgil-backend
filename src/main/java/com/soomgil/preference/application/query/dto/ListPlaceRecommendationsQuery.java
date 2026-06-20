package com.soomgil.preference.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.api.dto.RecommendationTab;
import java.util.UUID;

/**
 * 여행방의 현재 지도 viewport 안에서 추천 장소를 조회하는 query.
 *
 * <p>{@code bbox}는 필수이고 {@code page}는 0부터 시작한다. center 좌표는 거리 표시와
 * 동점 정렬에만 사용하며 추천 취향 점수에는 더하지 않는다.
 *
 * @param tripId 추천을 조회할 여행방 ID
 * @param bbox {@code minLng,minLat,maxLng,maxLat} 형식의 지도 경계
 * @param centerLat 선택적인 지도 중심 위도
 * @param centerLng 선택적인 지도 중심 경도
 * @param tab 기본 추천 또는 SUPER_LIKE 탭
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record ListPlaceRecommendationsQuery(
	UUID tripId,
	String bbox,
	Double centerLat,
	Double centerLng,
	RecommendationTab tab,
	int page,
	int size
) implements Query<PagedPlaceRecommendation> {
}
