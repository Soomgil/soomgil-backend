package com.soomgil.place.application.port;

/**
 * 한 지역(KTO 시도/시군구)에 속한 관광 원천 장소들의 좌표 범위와 중심.
 *
 * <p>여행지역만 정해지고 아직 담은 장소가 없는 여행방에서 지도를 그 지역으로 옮기는 데 쓴다.
 *
 * @param centerLat 중심 위도
 * @param centerLng 중심 경도
 * @param minLat    남쪽 경계 위도
 * @param minLng    서쪽 경계 경도
 * @param maxLat    북쪽 경계 위도
 * @param maxLng    동쪽 경계 경도
 * @param placeCount 범위 계산에 쓴 장소 수
 */
public record RegionViewport(
	double centerLat,
	double centerLng,
	double minLat,
	double minLng,
	double maxLat,
	double maxLng,
	long placeCount
) {
}
