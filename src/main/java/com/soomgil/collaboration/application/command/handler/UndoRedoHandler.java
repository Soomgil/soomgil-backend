package com.soomgil.collaboration.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.command.dto.UndoRedoAction;
import com.soomgil.collaboration.application.command.dto.UndoRedoCommand;
import com.soomgil.collaboration.application.command.dto.UndoRedoResult;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventReadModel;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.application.port.CollaborationCompensationExecutor;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 협업 command event stack을 기준으로 undo/redo 이벤트를 기록한다.
 */
@Component
public class UndoRedoHandler implements CommandHandler<UndoRedoCommand, UndoRedoResult> {

	private final CollaborationCommandEventRepository eventRepository;
	private final ItineraryCommandRepository itineraryRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;
	private final ObjectMapper objectMapper;
	private final List<CollaborationCompensationExecutor> compensationExecutors;

	public UndoRedoHandler(
		CollaborationCommandEventRepository eventRepository,
		ItineraryCommandRepository itineraryRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider,
		ObjectMapper objectMapper,
		List<CollaborationCompensationExecutor> compensationExecutors
	) {
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.itineraryRepository = Objects.requireNonNull(itineraryRepository, "itineraryRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.compensationExecutors = List.copyOf(compensationExecutors);
	}

	@Override
	@Transactional
	public UndoRedoResult handle(UndoRedoCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		CollaborationCommandEventReadModel candidate = findCandidate(command);

		Instant now = timeProvider.now();
		long newVersion = itineraryRepository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		executeCompensation(command, candidate, now);
		CollaborationCommandEvent event = command.action() == UndoRedoAction.UNDO
			? undoEvent(command, candidate, newVersion, now)
			: redoEvent(command, candidate, newVersion, now);
		Long eventId = eventRepository.saveReturningId(event);
		return new UndoRedoResult(
			command.tripId(),
			newVersion,
			eventId,
			eventRepository.hasUndoCandidate(command.tripId(), command.actorUserId(), command.websocketSessionId()),
			eventRepository.hasRedoCandidate(command.tripId(), command.actorUserId(), command.websocketSessionId())
		);
	}

	private void executeCompensation(
		UndoRedoCommand command,
		CollaborationCommandEventReadModel candidate,
		Instant now
	) {
		String payload = command.action() == UndoRedoAction.UNDO
			? candidate.inversePayload()
			: candidate.redoPayload();
		CollaborationCompensationExecutor executor = compensationExecutors.stream()
			.filter(candidateExecutor -> candidateExecutor.supports(payload))
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Collaboration command cannot be compensated."));
		executor.execute(command.tripId(), command.actorUserId(), payload, now);
	}

	private void validate(UndoRedoCommand command) {
		if (command.action() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Undo/redo action is required.");
		}
		if (command.websocketSessionId() == null || command.websocketSessionId().isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "WebSocket session ID is required.");
		}
	}

	private CollaborationCommandEventReadModel findCandidate(UndoRedoCommand command) {
		if (command.action() == UndoRedoAction.UNDO) {
			return eventRepository.findUndoCandidate(
				command.tripId(),
				command.actorUserId(),
				command.websocketSessionId(),
				command.commandEventId()
			).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Undoable command was not found."));
		}
		return eventRepository.findRedoCandidate(
			command.tripId(),
			command.actorUserId(),
			command.websocketSessionId(),
			command.commandEventId()
		).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Redoable command was not found."));
	}

	private CollaborationCommandEvent undoEvent(
		UndoRedoCommand command,
		CollaborationCommandEventReadModel candidate,
		long newVersion,
		Instant now
	) {
		return new CollaborationCommandEvent(
			command.tripId(),
			command.actorUserId(),
			command.websocketSessionId(),
			"UNDO",
			"UNDO_" + candidate.commandType(),
			candidate.aggregateType(),
			candidate.aggregateId(),
			command.baseVersion(),
			newVersion,
			stackPayload("UNDO", "targetEventId", candidate.id(), candidate.inversePayload()),
			candidate.inversePayload(),
			candidate.redoPayload() == null ? candidate.payload() : candidate.redoPayload(),
			now
		);
	}

	private CollaborationCommandEvent redoEvent(
		UndoRedoCommand command,
		CollaborationCommandEventReadModel candidate,
		long newVersion,
		Instant now
	) {
		return new CollaborationCommandEvent(
			command.tripId(),
			command.actorUserId(),
			command.websocketSessionId(),
			"REDO",
			candidate.commandType().replaceFirst("^UNDO_", "REDO_"),
			candidate.aggregateType(),
			candidate.aggregateId(),
			command.baseVersion(),
			newVersion,
			stackPayload("REDO", "targetUndoEventId", candidate.id(), candidate.redoPayload()),
			candidate.inversePayload(),
			candidate.redoPayload(),
			now
		);
	}

	private String stackPayload(String action, String targetKey, Long targetEventId, String commandPayload) {
		try {
			JsonNode command = commandPayload == null ? null : objectMapper.readTree(commandPayload);
			return objectMapper.writeValueAsString(Map.of(
				"action", action,
				targetKey, targetEventId,
				"command", command
			));
		}
		catch (JsonProcessingException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Collaboration command payload is invalid.");
		}
	}
}
