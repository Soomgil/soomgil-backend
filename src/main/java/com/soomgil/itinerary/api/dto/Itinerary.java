package com.soomgil.itinerary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record Itinerary(
	@NotNull
	UUID tripId,
	@NotNull
	Long itineraryVersion,
	@Valid
	@NotNull
	List<ItineraryDay> days,
	@Valid
	@NotNull
	List<RouteSegment> routes,
	@Valid
	@NotNull
	List<MapDrawing> mapDrawings
) {
}
