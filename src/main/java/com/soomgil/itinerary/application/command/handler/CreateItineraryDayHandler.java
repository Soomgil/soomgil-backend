package com.soomgil.itinerary.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryDayView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateItineraryDayCommand}를 처리해 일정 day를 생성한다.
 *
 * <p>active trip member 권한과 trip 단위 {@code baseVersion} 충돌을 같은 transaction에서 검증한다.
 */
@Component
public class CreateItineraryDayHandler implements CommandHandler<CreateItineraryDayCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public CreateItineraryDayHandler(
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
	public ItineraryMutationResult handle(CreateItineraryDayCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);

		if (command.groupType() == ItineraryDayGroupType.UNSCHEDULED) {
			return reuseUnscheduledDayIfPresent(command);
		}
		return createNewDay(command);
	}

	private void validate(CreateItineraryDayCommand command) {
		if (command.groupType() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day group type is required.");
		}
		if (command.groupType() == ItineraryDayGroupType.DAY && command.dayNumber() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day number is required for DAY group.");
		}
		if (command.groupType() == ItineraryDayGroupType.UNSCHEDULED && command.dayNumber() != null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Unscheduled group must not have day number.");
		}
		if (command.dayNumber() != null && command.dayNumber() < 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day number must be greater than or equal to 1.");
		}
		if (command.sortOrder() != null && command.sortOrder() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sort order must be greater than or equal to 0.");
		}
	}

	private ItineraryMutationResult reuseUnscheduledDayIfPresent(CreateItineraryDayCommand command) {
		return repository.findUnscheduledDay(command.tripId())
			.map(day -> {
				long currentVersion = repository.findItineraryVersion(command.tripId())
					.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found."));
				if (currentVersion != command.baseVersion()) {
					throw new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match.");
				}
				return toResult(command.tripId(), currentVersion, day);
			})
			.orElseGet(() -> createNewDay(command));
	}

	private ItineraryMutationResult createNewDay(CreateItineraryDayCommand command) {
		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		ItineraryDayCreate day = new ItineraryDayCreate(
			Ids.newUuid(),
			command.tripId(),
			command.groupType(),
			command.dayNumber(),
			command.date(),
			normalizeTitle(command.title()),
			command.sortOrder() == null ? 0 : command.sortOrder(),
			now,
			now
		);
		repository.insertDay(day);
		eventRepository.save(ItineraryCollaborationEvents.dayCreated(
			day,
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			new ItineraryDayView(
				day.id(),
				day.tripId(),
				day.groupType(),
				day.dayNumber(),
				day.date(),
				day.title(),
				day.sortOrder()
			),
			null,
			null,
			null,
			List.of()
		);
	}

	private ItineraryMutationResult toResult(UUID tripId, long version, ItineraryDayReadModel day) {
		return new ItineraryMutationResult(
			tripId,
			version,
			new ItineraryDayView(
				day.id(),
				day.tripId(),
				day.groupType(),
				day.dayNumber(),
				day.date(),
				day.title(),
				day.sortOrder()
			),
			null,
			null,
			null,
			List.of()
		);
	}

	private String normalizeTitle(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
