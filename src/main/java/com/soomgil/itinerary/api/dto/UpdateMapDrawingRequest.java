package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateMapDrawingRequest(
	@NotNull
	Long baseVersion,
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	Integer sortOrder,
	Long drawingVersion
) {
}
