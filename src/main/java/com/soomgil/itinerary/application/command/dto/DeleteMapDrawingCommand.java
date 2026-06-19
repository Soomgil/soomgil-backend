package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * map drawing 삭제 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param drawingId 삭제할 drawing ID
 */
public record DeleteMapDrawingCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID drawingId
) implements Command<ItineraryMutationResult> {
}
