package com.soomgil.itinerary.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record ItineraryItem(
	@NotNull
	UUID id,
	@NotNull
	UUID itineraryDayId,
	@NotNull
	Integer sortOrder,
	@NotNull
	ItineraryItemType itemType,
	@Valid
	@JsonAlias("placeRef")
	PlaceRef place,
	@NotBlank
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	@NotNull
	PlaceSourceStatus sourceStatus
) {
}
