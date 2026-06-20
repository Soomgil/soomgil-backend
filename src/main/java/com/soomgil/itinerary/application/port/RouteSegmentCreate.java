package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * itinerary.trip_routes 추가 모델.
 */
public record RouteSegmentCreate(
	UUID id,
	UUID tripId,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	RouteMode mode,
	String provider,
	String providerProfile,
	GeometryFormat geometryFormat,
	String geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence,
	UUID createdByUserId,
	UUID updatedByUserId,
	Instant createdAt,
	Instant updatedAt
) {

	public RouteSegmentCreate {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(originItineraryItemId, "originItineraryItemId must not be null");
		Objects.requireNonNull(destinationItineraryItemId, "destinationItineraryItemId must not be null");
		Objects.requireNonNull(mode, "mode must not be null");
		Objects.requireNonNull(provider, "provider must not be null");
		Objects.requireNonNull(providerProfile, "providerProfile must not be null");
		Objects.requireNonNull(geometryFormat, "geometryFormat must not be null");
		Objects.requireNonNull(geometry, "geometry must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
