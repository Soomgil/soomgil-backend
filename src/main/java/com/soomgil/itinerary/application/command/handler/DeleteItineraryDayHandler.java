package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.DeleteItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteItineraryDayCommand}를 처리해 비어 있는 itinerary day를 삭제한다.
 */
@Component
public class DeleteItineraryDayHandler implements CommandHandler<DeleteItineraryDayCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public DeleteItineraryDayHandler(
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
	public ItineraryMutationResult handle(DeleteItineraryDayCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		if (command.dayId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day id is required.");
		}
		if (repository.findDay(command.tripId(), command.dayId()).isEmpty()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found.");
		}
		if (repository.countActiveItemsByDay(command.tripId(), command.dayId()) > 0) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Itinerary day must be empty before deletion.");
		}

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		boolean deleted = repository.deleteDay(command.tripId(), command.dayId());
		if (!deleted) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found.");
		}
		eventRepository.save(ItineraryCollaborationEvents.dayDeleted(
			command.tripId(),
			command.dayId(),
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
