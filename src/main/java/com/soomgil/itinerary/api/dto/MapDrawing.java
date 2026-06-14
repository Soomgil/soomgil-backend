package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record MapDrawing(
	@NotNull
	UUID id,
	UUID itineraryDayId,
	@NotNull
	DrawingType drawingType,
	@NotNull
	GeometryFormat geometryFormat,
	@NotNull
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	Integer sortOrder,
	@NotNull
	Long version
) {
}
