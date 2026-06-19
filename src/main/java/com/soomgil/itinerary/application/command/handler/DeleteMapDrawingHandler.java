package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.DeleteMapDrawingCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteMapDrawingCommand}를 처리해 map drawing을 soft delete한다.
 */
@Component
public class DeleteMapDrawingHandler implements CommandHandler<DeleteMapDrawingCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public DeleteMapDrawingHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public ItineraryMutationResult handle(DeleteMapDrawingCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		if (command.drawingId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Drawing id is required.");
		}
		if (!repository.existsActiveMapDrawing(command.tripId(), command.drawingId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
		}

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		boolean deleted = repository.softDeleteMapDrawing(
			command.tripId(),
			command.drawingId(),
			command.actorUserId(),
			now
		);
		if (!deleted) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
		}
		eventRepository.save(ItineraryCollaborationEvents.mapDrawingDeleted(
			command.tripId(),
			command.drawingId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			null,
			null,
			null,
			List.of()
		);
	}
}
