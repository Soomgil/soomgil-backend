package com.soomgil.place.api.dto;

/**
 * 여행지역의 지도 뷰포트(중심과 경계). 담은 장소가 없는 여행방에서 지도를 그 지역으로 옮기는 데 쓴다.
 */
public record RegionViewportResponse(
	double centerLat,
	double centerLng,
	double minLat,
	double minLng,
	double maxLat,
	double maxLng,
	long placeCount
) {
}
