package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.util.Map;
import java.util.UUID;

/**
 * provider에서 확정된 route segment 저장 command.
 *
 * <p>Mapbox map matching 성공 결과처럼 최종 snapped geometry가 있는 경우에만 호출한다.
 */
public record SaveRouteSegmentCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	RouteMode mode,
	String provider,
	String providerProfile,
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) implements Command<ItineraryMutationResult> {
}
