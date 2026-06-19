package com.soomgil.itinerary.application.command.dto;

import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 일정 변경 응답에 포함되는 저장 지도 도형 view.
 */
public record MapDrawingView(
	UUID id,
	UUID itineraryDayId,
	DrawingType drawingType,
	GeometryFormat geometryFormat,
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	int sortOrder,
	long version
) {
}
