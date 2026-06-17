package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * route segment 삭제 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param routeId 삭제할 route ID
 */
public record DeleteRouteSegmentCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID routeId
) implements Command<ItineraryMutationResult> {
}
