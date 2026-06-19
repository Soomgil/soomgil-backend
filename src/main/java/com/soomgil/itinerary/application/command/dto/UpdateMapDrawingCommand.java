package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.Map;
import java.util.UUID;

/**
 * map drawing 수정 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param drawingId 수정할 drawing ID
 * @param geometry GeoJSON geometry
 * @param style style JSON
 * @param label 표시 label
 * @param sortOrder 정렬 순서
 * @param drawingVersion 요청자가 본 drawing version
 */
public record UpdateMapDrawingCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID drawingId,
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	Integer sortOrder,
	Long drawingVersion
) implements Command<ItineraryMutationResult> {
}
