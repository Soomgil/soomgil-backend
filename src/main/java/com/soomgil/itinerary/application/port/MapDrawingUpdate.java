package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * itinerary.map_drawings 수정 모델.
 *
 * @param tripId 여행방 ID
 * @param drawingId drawing ID
 * @param geometry 변경할 geometry JSON
 * @param style 변경할 style JSON
 * @param label 변경할 label
 * @param sortOrder 변경할 정렬 순서
 * @param expectedVersion 요청자가 본 drawing version
 * @param updatedByUserId 수정 사용자 ID
 * @param updatedAt 수정 시각
 */
public record MapDrawingUpdate(
	UUID tripId,
	UUID drawingId,
	String geometry,
	String style,
	String label,
	Integer sortOrder,
	Long expectedVersion,
	UUID updatedByUserId,
	Instant updatedAt
) {
}
