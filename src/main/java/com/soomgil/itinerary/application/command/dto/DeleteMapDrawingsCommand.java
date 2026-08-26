package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.List;
import java.util.UUID;

/**
 * 하나의 사용자 제스처에 포함된 map drawing들을 일괄 삭제하는 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param drawingIds 함께 삭제하고 함께 undo할 drawing ID 목록
 * @param websocketSessionId 모든 대상의 삭제 lease를 소유한 WebSocket session ID
 */
public record DeleteMapDrawingsCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	List<UUID> drawingIds,
	String websocketSessionId
) implements Command<ItineraryMutationResult> {
}
