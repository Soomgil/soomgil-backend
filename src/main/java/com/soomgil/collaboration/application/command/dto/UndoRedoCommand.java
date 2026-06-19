package com.soomgil.collaboration.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * 협업 undo/redo 실행 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param websocketSessionId undo/redo stack을 귀속할 WebSocket session ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param commandEventId 특정 command event ID
 * @param action undo 또는 redo
 */
public record UndoRedoCommand(
	UUID tripId,
	UUID actorUserId,
	String websocketSessionId,
	long baseVersion,
	Long commandEventId,
	UndoRedoAction action
) implements Command<UndoRedoResult> {
}
