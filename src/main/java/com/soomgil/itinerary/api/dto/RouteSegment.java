package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record RouteSegment(
	@NotNull
	UUID id,
	@NotNull
	UUID originItineraryItemId,
	@NotNull
	UUID destinationItineraryItemId,
	@NotNull
	RouteMode mode,
	@NotBlank
	String provider,
	String providerProfile,
	@NotNull
	GeometryFormat geometryFormat,
	@NotNull
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) {
}
