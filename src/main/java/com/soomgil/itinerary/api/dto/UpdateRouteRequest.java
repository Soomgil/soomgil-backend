package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateRouteRequest(
	@NotNull
	Long baseVersion,
	RouteMode mode,
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) {
}
