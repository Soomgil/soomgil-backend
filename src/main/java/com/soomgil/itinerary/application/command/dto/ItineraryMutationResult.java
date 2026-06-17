package com.soomgil.itinerary.application.command.dto;

import java.util.List;
import java.util.UUID;

/**
 * 일정 쓰기 command 처리 결과.
 *
 * <p>{@code itineraryVersion}은 성공한 write 이후 증가된 trip 단위 협업 version이다.
 */
public record ItineraryMutationResult(
	UUID tripId,
	long itineraryVersion,
	ItineraryDayView day,
	ItineraryItemView item,
	List<UUID> affectedRouteIds
) {

	public ItineraryMutationResult {
		affectedRouteIds = affectedRouteIds == null ? List.of() : List.copyOf(affectedRouteIds);
	}
}
