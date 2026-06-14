package com.soomgil.itinerary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MapMatchRouteResponse(
	@NotNull
	UUID tripId,
	@NotNull
	Long itineraryVersion,
	@Valid
	ItineraryDay day,
	@Valid
	ItineraryItem item,
	@Valid
	RouteSegment route,
	@Valid
	MapDrawing drawing,
	List<UUID> affectedRouteIds,
	Long matchRequestId,
	List<Map<String, Object>> tracepoints,
	Map<String, Object> matchingsMetadata
) {
}
