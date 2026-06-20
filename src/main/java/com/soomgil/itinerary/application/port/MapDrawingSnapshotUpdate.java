package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * undo/redo용 map drawing 전체 mutable 상태 복원 모델.
 */
public record MapDrawingSnapshotUpdate(
	UUID tripId,
	UUID drawingId,
	String geometry,
	String style,
	String label,
	Integer sortOrder,
	UUID updatedByUserId,
	Instant updatedAt
) {
}
