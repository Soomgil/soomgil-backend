package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.RouteMode;
import java.time.Instant;
import java.util.UUID;

/**
 * undo/redo용 route segment 전체 mutable 상태 복원 모델.
 */
public record RouteSegmentSnapshotUpdate(
	UUID tripId,
	UUID routeId,
	RouteMode mode,
	String providerProfile,
	String geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence,
	UUID updatedByUserId,
	Instant updatedAt
) {
}
