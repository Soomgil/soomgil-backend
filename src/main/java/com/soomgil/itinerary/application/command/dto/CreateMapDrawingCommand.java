package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.domain.model.DrawingType;
import java.util.Map;
import java.util.UUID;

/**
 * 저장된 지도 도형 생성을 요청하는 command.
 *
 * <p>실시간 preview stroke는 저장하지 않고, 사용자가 명시적으로 저장한 geometry만 처리한다.
 */
public record CreateMapDrawingCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID itineraryDayId,
	DrawingType drawingType,
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	Integer sortOrder
) implements Command<ItineraryMutationResult> {
}
