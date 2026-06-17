package com.soomgil.itinerary.application.command.dto;

import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.net.URI;
import java.util.UUID;

/**
 * 일정 변경 응답에 포함되는 item view.
 */
public record ItineraryItemView(
	UUID id,
	UUID itineraryDayId,
	int sortOrder,
	ItineraryItemType itemType,
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String sourceStatus
) {
}
