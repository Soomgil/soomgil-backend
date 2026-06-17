package com.soomgil.itinerary.application.command.dto;

import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.util.Map;
import java.util.UUID;

/**
 * 일정 변경 응답에 포함되는 route segment view.
 */
public record RouteSegmentView(
	UUID id,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	RouteMode mode,
	String provider,
	String providerProfile,
	GeometryFormat geometryFormat,
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) {
}
