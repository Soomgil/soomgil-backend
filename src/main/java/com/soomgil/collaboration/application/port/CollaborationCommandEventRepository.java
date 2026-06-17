package com.soomgil.collaboration.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * 협업 command event 쓰기 persistence 계약.
 */
public interface CollaborationCommandEventRepository {

	/**
	 * 협업 write command event를 저장한다.
	 *
	 * @param event 저장할 event
	 */
	void save(CollaborationCommandEvent event);

	/**
	 * 협업 write command event를 저장하고 생성 ID를 반환한다.
	 *
	 * @param event 저장할 event
	 * @return 생성된 event ID
	 */
	default Long saveReturningId(CollaborationCommandEvent event) {
		save(event);
		return null;
	}

	/**
	 * undo 가능한 command event를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param actorUserId 요청 사용자 ID
	 * @param websocketSessionId WebSocket session ID
	 * @param commandEventId 특정 event ID
	 * @return undo 후보
	 */
	default Optional<CollaborationCommandEventReadModel> findUndoCandidate(
		UUID tripId,
		UUID actorUserId,
		String websocketSessionId,
		Long commandEventId
	) {
		return Optional.empty();
	}

	/**
	 * redo 가능한 undo event를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param actorUserId 요청 사용자 ID
	 * @param websocketSessionId WebSocket session ID
	 * @param commandEventId 특정 undo event ID
	 * @return redo 후보
	 */
	default Optional<CollaborationCommandEventReadModel> findRedoCandidate(
		UUID tripId,
		UUID actorUserId,
		String websocketSessionId,
		Long commandEventId
	) {
		return Optional.empty();
	}

	/**
	 * undo 가능 여부를 조회한다.
	 */
	default boolean hasUndoCandidate(UUID tripId, UUID actorUserId, String websocketSessionId) {
		return false;
	}

	/**
	 * redo 가능 여부를 조회한다.
	 */
	default boolean hasRedoCandidate(UUID tripId, UUID actorUserId, String websocketSessionId) {
		return false;
	}
}
