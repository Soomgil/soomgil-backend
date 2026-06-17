package com.soomgil.collaboration.infrastructure.persistence.mapper;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventReadModel;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 협업 command event SQL mapper.
 */
@Mapper
public interface CollaborationCommandEventMapper {

	/**
	 * 협업 command event row를 추가한다.
	 *
	 * @param event 저장할 event
	 */
	void insertEvent(@Param("event") CollaborationCommandEvent event);

	/**
	 * 협업 command event row를 추가하고 ID를 반환한다.
	 *
	 * @param event 저장할 event
	 * @return 생성 ID
	 */
	Long insertEventReturningId(@Param("event") CollaborationCommandEvent event);

	CollaborationCommandEventReadModel findUndoCandidate(
		@Param("tripId") UUID tripId,
		@Param("actorUserId") UUID actorUserId,
		@Param("websocketSessionId") String websocketSessionId,
		@Param("commandEventId") Long commandEventId
	);

	CollaborationCommandEventReadModel findRedoCandidate(
		@Param("tripId") UUID tripId,
		@Param("actorUserId") UUID actorUserId,
		@Param("websocketSessionId") String websocketSessionId,
		@Param("commandEventId") Long commandEventId
	);

	boolean hasUndoCandidate(
		@Param("tripId") UUID tripId,
		@Param("actorUserId") UUID actorUserId,
		@Param("websocketSessionId") String websocketSessionId
	);

	boolean hasRedoCandidate(
		@Param("tripId") UUID tripId,
		@Param("actorUserId") UUID actorUserId,
		@Param("websocketSessionId") String websocketSessionId
	);
}
