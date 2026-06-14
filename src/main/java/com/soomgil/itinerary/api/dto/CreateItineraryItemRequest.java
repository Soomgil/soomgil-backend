package com.soomgil.itinerary.api.dto;

import com.soomgil.place.api.dto.PlaceRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record CreateItineraryItemRequest(
	@NotNull
	Long baseVersion,
	@NotNull
	UUID itineraryDayId,
	@NotNull
	Integer sortOrder,
	@NotNull
	ItineraryItemType itemType,
	@Valid
	PlaceRef place,
	@NotBlank
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl
) {
}
