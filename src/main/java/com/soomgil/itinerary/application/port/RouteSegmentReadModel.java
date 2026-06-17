package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.util.Map;
import java.util.UUID;

/**
 * route segment 조회 모델.
 *
 * @param id route ID
 * @param originItineraryItemId 출발 item ID
 * @param destinationItineraryItemId 도착 item ID
 * @param mode 이동 mode
 * @param provider provider 이름
 * @param providerProfile provider profile
 * @param geometryFormat geometry 형식
 * @param geometry GeoJSON geometry
 * @param distanceMeters 거리(m)
 * @param durationSeconds 소요 시간(초)
 * @param confidence matching 신뢰도
 */
public record RouteSegmentReadModel(
	UUID id,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	RouteMode mode,
	String provider,
	String providerProfile,
	GeometryFormat geometryFormat,
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) {
}
