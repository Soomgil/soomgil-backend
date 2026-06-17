package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * itinerary day 삭제 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param dayId 삭제할 day ID
 */
public record DeleteItineraryDayCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID dayId
) implements Command<ItineraryMutationResult> {
}
