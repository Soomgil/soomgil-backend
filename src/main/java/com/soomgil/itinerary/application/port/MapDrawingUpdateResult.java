package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import java.util.UUID;

/**
 * map drawing 수정 결과 모델.
 *
 * @param id drawing ID
 * @param itineraryDayId 연결 day ID
 * @param drawingType drawing 타입
 * @param geometryFormat geometry 형식
 * @param geometry GeoJSON geometry JSON
 * @param style style JSON
 * @param label 표시 label
 * @param sortOrder 정렬 순서
 * @param version drawing version
 */
public record MapDrawingUpdateResult(
	UUID id,
	UUID itineraryDayId,
	DrawingType drawingType,
	GeometryFormat geometryFormat,
	String geometry,
	String style,
	String label,
	Integer sortOrder,
	Long version
) {
}
