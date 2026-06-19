package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.RouteMode;
import java.time.Instant;
import java.util.UUID;

/**
 * itinerary.trip_routes 수정 모델.
 *
 * @param tripId 여행방 ID
 * @param routeId route ID
 * @param mode 변경할 이동 mode
 * @param providerProfile 변경할 provider profile
 * @param geometry 변경할 GeoJSON geometry JSON
 * @param distanceMeters 변경할 거리(m)
 * @param durationSeconds 변경할 소요 시간(초)
 * @param confidence 변경할 matching 신뢰도
 * @param updatedByUserId 수정 사용자 ID
 * @param updatedAt 수정 시각
 */
public record RouteSegmentUpdate(
	UUID tripId,
	UUID routeId,
	RouteMode mode,
	String providerProfile,
	String geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence,
	UUID updatedByUserId,
	Instant updatedAt
) {
}
