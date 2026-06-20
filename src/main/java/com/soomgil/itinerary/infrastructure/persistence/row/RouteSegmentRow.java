package com.soomgil.itinerary.infrastructure.persistence.row;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * route segment 조회 row.
 */
public record RouteSegmentRow(
	UUID id,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	String mode,
	String provider,
	String providerProfile,
	String geometryFormat,
	String geometry,
	BigDecimal distanceMeters,
	BigDecimal durationSeconds,
	BigDecimal confidence
) {
}
