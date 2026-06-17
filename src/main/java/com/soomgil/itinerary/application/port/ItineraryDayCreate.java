package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * itinerary.itinerary_days 추가 모델.
 */
public record ItineraryDayCreate(
	UUID id,
	UUID tripId,
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	int sortOrder,
	Instant createdAt,
	Instant updatedAt
) {

	public ItineraryDayCreate {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(groupType, "groupType must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
