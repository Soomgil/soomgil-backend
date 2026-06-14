package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record UpdateItineraryItemRequest(
	@NotNull
	Long baseVersion,
	UUID itineraryDayId,
	Integer sortOrder,
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl
) {
}
