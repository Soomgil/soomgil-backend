package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CreateMapDrawingRequest(
	@NotNull
	Long baseVersion,
	UUID itineraryDayId,
	@NotNull
	DrawingType drawingType,
	@NotNull
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	Integer sortOrder
) {
}
