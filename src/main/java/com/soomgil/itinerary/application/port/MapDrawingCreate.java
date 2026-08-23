package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * itinerary.map_drawings 추가 모델.
 *
 * <p>STICKER는 {@code stickerCode}, IMAGE는 {@code mediaFileId}만 가지며
 * 두 타입 모두 meter 단위 크기를 담은 {@code transform} JSON이 필요하다.
 */
public record MapDrawingCreate(
	UUID id,
	UUID tripId,
	UUID itineraryDayId,
	DrawingType drawingType,
	GeometryFormat geometryFormat,
	String geometry,
	String style,
	String label,
	UUID mediaFileId,
	String stickerCode,
	String transform,
	int sortOrder,
	long version,
	UUID createdByUserId,
	UUID updatedByUserId,
	Instant createdAt,
	Instant updatedAt
) {

	public MapDrawingCreate {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(drawingType, "drawingType must not be null");
		Objects.requireNonNull(geometryFormat, "geometryFormat must not be null");
		Objects.requireNonNull(geometry, "geometry must not be null");
		Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public MapDrawingCreate(
		UUID id,
		UUID tripId,
		UUID itineraryDayId,
		DrawingType drawingType,
		GeometryFormat geometryFormat,
		String geometry,
		String style,
		String label,
		int sortOrder,
		long version,
		UUID createdByUserId,
		UUID updatedByUserId,
		Instant createdAt,
		Instant updatedAt
	) {
		this(id, tripId, itineraryDayId, drawingType, geometryFormat, geometry, style, label, null, null, null,
			sortOrder, version, createdByUserId, updatedByUserId, createdAt, updatedAt);
	}
}
