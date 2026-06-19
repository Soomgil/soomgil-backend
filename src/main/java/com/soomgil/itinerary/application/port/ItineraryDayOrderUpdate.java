package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * itinerary day sort order 갱신 모델.
 */
public record ItineraryDayOrderUpdate(
	UUID tripId,
	UUID dayId,
	int sortOrder,
	Instant updatedAt
) {

	public ItineraryDayOrderUpdate {
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(dayId, "dayId must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
