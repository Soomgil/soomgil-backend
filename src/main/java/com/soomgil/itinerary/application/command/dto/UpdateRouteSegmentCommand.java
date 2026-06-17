package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.util.Map;
import java.util.UUID;

/**
 * route segment 수정 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param routeId 수정할 route ID
 * @param mode 변경할 이동 mode
 * @param geometry 변경할 GeoJSON geometry
 * @param distanceMeters 변경할 거리(m)
 * @param durationSeconds 변경할 소요 시간(초)
 * @param confidence 변경할 matching 신뢰도
 */
public record UpdateRouteSegmentCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID routeId,
	RouteMode mode,
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) implements Command<ItineraryMutationResult> {
}
