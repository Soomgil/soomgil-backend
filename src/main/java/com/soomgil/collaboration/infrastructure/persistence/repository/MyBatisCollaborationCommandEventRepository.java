package com.soomgil.collaboration.infrastructure.persistence.repository;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventReadModel;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.application.port.CollaborationEventBroadcaster;
import com.soomgil.collaboration.application.port.CollaborationSessionIdProvider;
import com.soomgil.collaboration.infrastructure.persistence.mapper.CollaborationCommandEventMapper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * MyBatis 기반 협업 command event repository.
 */
@Repository
public class MyBatisCollaborationCommandEventRepository implements CollaborationCommandEventRepository {

	private final CollaborationCommandEventMapper mapper;
	private final CollaborationEventBroadcaster broadcaster;
	private final CollaborationSessionIdProvider sessionIdProvider;

	public MyBatisCollaborationCommandEventRepository(
		CollaborationCommandEventMapper mapper,
		CollaborationEventBroadcaster broadcaster,
		CollaborationSessionIdProvider sessionIdProvider
	) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster must not be null");
		this.sessionIdProvider = Objects.requireNonNull(sessionIdProvider, "sessionIdProvider must not be null");
	}

	@Override
	public void save(CollaborationCommandEvent event) {
		saveReturningId(event);
	}

	@Override
	public Long saveReturningId(CollaborationCommandEvent event) {
		CollaborationCommandEvent persistedEvent = assignCurrentSession(event);
		Long commandEventId = mapper.insertEventReturningId(persistedEvent);
		broadcastAfterCommit(commandEventId, persistedEvent);
		return commandEventId;
	}

	private void broadcastAfterCommit(Long commandEventId, CollaborationCommandEvent event) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			broadcaster.broadcast(commandEventId, event);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				broadcaster.broadcast(commandEventId, event);
			}
		});
	}

	private CollaborationCommandEvent assignCurrentSession(CollaborationCommandEvent event) {
		String sessionId = event.websocketSessionId() == null
			? sessionIdProvider.currentSessionId()
			: event.websocketSessionId();
		if (Objects.equals(sessionId, event.websocketSessionId())) {
			return event;
		}
		return new CollaborationCommandEvent(
			event.tripId(),
			event.actorUserId(),
			sessionId,
			event.source(),
			event.commandType(),
			event.aggregateType(),
			event.aggregateId(),
			event.versionBefore(),
			event.versionAfter(),
			event.payload(),
			event.inversePayload(),
			event.redoPayload(),
			event.createdAt()
		);
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
