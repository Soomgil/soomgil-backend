package com.soomgil.itinerary.infrastructure.persistence.row;

import java.util.UUID;

/**
 * map drawing 조회 row.
 */
public record MapDrawingRow(
	UUID id,
	UUID itineraryDayId,
	String drawingType,
	String geometryFormat,
	String geometry,
	String style,
	String label,
	UUID mediaFileId,
	String stickerCode,
	String transform,
	Integer sortOrder,
	Long version
) {
	public MapDrawingRow(
		UUID id,
		UUID itineraryDayId,
		String drawingType,
		String geometryFormat,
		String geometry,
		String style,
		String label,
		Integer sortOrder,
		Long version
	) {
		this(id, itineraryDayId, drawingType, geometryFormat, geometry, style, label, null, null, null,
			sortOrder, version);
	}
}
