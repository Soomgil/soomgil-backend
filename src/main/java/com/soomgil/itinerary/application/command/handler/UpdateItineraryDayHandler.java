package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryDayView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryDayCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryDayUpdate;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateItineraryDayCommand}를 처리해 itinerary day를 수정한다.
 */
@Component
public class UpdateItineraryDayHandler implements CommandHandler<UpdateItineraryDayCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public UpdateItineraryDayHandler(
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
	public ItineraryMutationResult handle(UpdateItineraryDayCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validateRequest(command);
		ItineraryDayReadModel current = repository.findDay(command.tripId(), command.dayId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found."));
		ItineraryDayUpdate update = toUpdate(command, current);

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		ItineraryDayReadModel updated = repository.updateDay(new ItineraryDayUpdate(
			update.tripId(),
			update.dayId(),
			update.dayNumber(),
			update.date(),
			update.title(),
			update.sortOrder(),
			now
		)).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found."));
		eventRepository.save(ItineraryCollaborationEvents.dayUpdated(
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
			toView(updated),
			null,
			null,
			null,
			List.of()
		);
	}

	private void validateRequest(UpdateItineraryDayCommand command) {
		if (command.dayId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day id is required.");
		}
		if (command.dayNumber() == null && command.date() == null && command.title() == null && command.sortOrder() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "At least one day field is required.");
		}
		if (command.dayNumber() != null && command.dayNumber() < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day number must be greater than or equal to 1.");
		}
		if (command.sortOrder() != null && command.sortOrder() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sort order must be greater than or equal to 0.");
		}
	}

	private ItineraryDayUpdate toUpdate(UpdateItineraryDayCommand command, ItineraryDayReadModel current) {
		Integer dayNumber = command.dayNumber() == null ? current.dayNumber() : command.dayNumber();
		LocalDate date = command.date() == null ? current.date() : command.date();
		String title = command.title() == null ? current.title() : normalizeTitle(command.title());
		Integer sortOrder = command.sortOrder() == null ? current.sortOrder() : command.sortOrder();
		if (current.groupType() == ItineraryDayGroupType.UNSCHEDULED && (dayNumber != null || date != null)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Unscheduled group must not have day number or date.");
		}
		if (current.groupType() == ItineraryDayGroupType.DAY && dayNumber == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day number is required for DAY group.");
		}
		return new ItineraryDayUpdate(command.tripId(), command.dayId(), dayNumber, date, title, sortOrder, null);
	}

	private ItineraryDayView toView(ItineraryDayReadModel day) {
		return new ItineraryDayView(
			day.id(),
			day.tripId(),
			day.groupType(),
			day.dayNumber(),
			day.date(),
			day.title(),
			day.sortOrder()
		);
	}

	private String normalizeTitle(String value) {
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
