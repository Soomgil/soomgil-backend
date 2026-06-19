package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * itinerary item의 day 소속과 sort order 갱신 모델.
 */
public record ItineraryItemOrderUpdate(
	UUID tripId,
	UUID dayId,
	UUID itemId,
	int sortOrder,
	UUID updatedByUserId,
	Instant updatedAt
) {

	public ItineraryItemOrderUpdate {
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(dayId, "dayId must not be null");
		Objects.requireNonNull(itemId, "itemId must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
