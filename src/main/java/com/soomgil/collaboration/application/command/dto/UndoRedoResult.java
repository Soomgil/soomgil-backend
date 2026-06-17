package com.soomgil.collaboration.application.command.dto;

import java.util.UUID;

/**
 * undo/redo 처리 결과.
 *
 * @param tripId 여행방 ID
 * @param itineraryVersion 처리 후 itinerary version
 * @param commandEventId 생성된 undo/redo event ID
 * @param undoAvailable 다음 undo 가능 여부
 * @param redoAvailable 다음 redo 가능 여부
 */
public record UndoRedoResult(
	UUID tripId,
	long itineraryVersion,
	Long commandEventId,
	boolean undoAvailable,
	boolean redoAvailable
) {
}
