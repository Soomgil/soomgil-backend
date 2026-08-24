package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.domain.model.DrawingType;
import java.util.Map;
import java.util.UUID;

/**
 * 저장된 지도 도형 생성을 요청하는 command.
 *
 * <p>실시간 preview stroke는 저장하지 않고, 사용자가 명시적으로 저장한 geometry만 처리한다.
 * STICKER와 IMAGE의 transform 크기는 meter 단위이며 Point geometry 중심과 일치해야 한다.
 *
 * @param websocketSessionId 협업 undo/redo stack을 귀속할 WebSocket session ID
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
	UUID mediaFileId,
	String stickerCode,
	Map<String, Object> transform,
	Integer sortOrder,
	String websocketSessionId
) implements Command<ItineraryMutationResult> {
	public CreateMapDrawingCommand(
		UUID tripId,
		UUID actorUserId,
		long baseVersion,
		UUID itineraryDayId,
		DrawingType drawingType,
		Map<String, Object> geometry,
		Map<String, Object> style,
		String label,
		UUID mediaFileId,
		String stickerCode,
		Map<String, Object> transform,
		Integer sortOrder
	) {
		this(tripId, actorUserId, baseVersion, itineraryDayId, drawingType, geometry, style, label,
			mediaFileId, stickerCode, transform, sortOrder, null);
	}

	public CreateMapDrawingCommand(
		UUID tripId,
		UUID actorUserId,
		long baseVersion,
		UUID itineraryDayId,
		DrawingType drawingType,
		Map<String, Object> geometry,
		Map<String, Object> style,
		String label,
		Integer sortOrder
	) {
		this(tripId, actorUserId, baseVersion, itineraryDayId, drawingType, geometry, style, label,
			null, null, null, sortOrder, null);
	}
}
