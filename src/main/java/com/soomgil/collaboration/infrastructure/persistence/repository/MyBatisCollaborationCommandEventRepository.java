package com.soomgil.collaboration.infrastructure.persistence.repository;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventReadModel;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
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

	public MyBatisCollaborationCommandEventRepository(CollaborationCommandEventMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public void save(CollaborationCommandEvent event) {
		mapper.insertEvent(event);
	}

	@Override
	public Long saveReturningId(CollaborationCommandEvent event) {
		return mapper.insertEventReturningId(event);
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
