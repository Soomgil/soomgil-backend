package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * itinerary.itinerary_items 추가 모델.
 */
public record ItineraryItemCreate(
	UUID id,
	UUID tripId,
	UUID itineraryDayId,
	int sortOrder,
	ItineraryItemType itemType,
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String sourceStatus,
	UUID createdByUserId,
	UUID updatedByUserId,
	Instant createdAt,
	Instant updatedAt
) {

	public ItineraryItemCreate {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(itineraryDayId, "itineraryDayId must not be null");
		Objects.requireNonNull(itemType, "itemType must not be null");
		Objects.requireNonNull(placeName, "placeName must not be null");
		Objects.requireNonNull(sourceStatus, "sourceStatus must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
