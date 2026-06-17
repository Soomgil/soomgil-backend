package com.soomgil.collaboration.infrastructure.persistence.repository;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventReadModel;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.application.port.CollaborationEventBroadcaster;
import com.soomgil.collaboration.infrastructure.persistence.mapper.CollaborationCommandEventMapper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 협업 command event repository.
 */
@Repository
public class MyBatisCollaborationCommandEventRepository implements CollaborationCommandEventRepository {

	private final CollaborationCommandEventMapper mapper;
	private final CollaborationEventBroadcaster broadcaster;

	public MyBatisCollaborationCommandEventRepository(
		CollaborationCommandEventMapper mapper,
		CollaborationEventBroadcaster broadcaster
	) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster must not be null");
	}

	@Override
	public void save(CollaborationCommandEvent event) {
		saveReturningId(event);
	}

	@Override
	public Long saveReturningId(CollaborationCommandEvent event) {
		Long commandEventId = mapper.insertEventReturningId(event);
		broadcaster.broadcast(commandEventId, event);
		return commandEventId;
	}

	@Override
	public Optional<CollaborationCommandEventReadModel> findUndoCandidate(
		UUID tripId,
		UUID actorUserId,
		String websocketSessionId,
		Long commandEventId
	) {
		return Optional.ofNullable(mapper.findUndoCandidate(tripId, actorUserId, websocketSessionId, commandEventId));
	}

	@Override
	public Optional<CollaborationCommandEventReadModel> findRedoCandidate(
		UUID tripId,
		UUID actorUserId,
		String websocketSessionId,
		Long commandEventId
	) {
		return Optional.ofNullable(mapper.findRedoCandidate(tripId, actorUserId, websocketSessionId, commandEventId));
	}

	@Override
	public boolean hasUndoCandidate(UUID tripId, UUID actorUserId, String websocketSessionId) {
		return mapper.hasUndoCandidate(tripId, actorUserId, websocketSessionId);
	}

	@Override
	public boolean hasRedoCandidate(UUID tripId, UUID actorUserId, String websocketSessionId) {
		return mapper.hasRedoCandidate(tripId, actorUserId, websocketSessionId);
	}
}
