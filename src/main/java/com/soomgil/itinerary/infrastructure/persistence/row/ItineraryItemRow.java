package com.soomgil.itinerary.infrastructure.persistence.row;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * itinerary item 조회 row.
 */
public record ItineraryItemRow(
	UUID id,
	UUID itineraryDayId,
	Integer sortOrder,
	String itemType,
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	BigDecimal lat,
	BigDecimal lng,
	String thumbnailUrl,
	String sourceStatus
) {
}
