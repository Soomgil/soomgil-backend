package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * itinerary item 삭제 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param itemId 삭제할 item ID
 */
public record DeleteItineraryItemCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID itemId
) implements Command<ItineraryMutationResult> {
}
