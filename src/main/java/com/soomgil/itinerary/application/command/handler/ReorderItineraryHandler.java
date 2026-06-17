package com.soomgil.itinerary.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryDayOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.ReorderItineraryCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ReorderItineraryCommand}를 처리해 일정 day와 item 순서를 갱신한다.
 *
 * <p>요청 snapshot의 모든 day/item은 같은 trip에 속해야 하며, 성공 시 trip itinerary version을 1 증가시킨다.
 */
@Component
public class ReorderItineraryHandler implements CommandHandler<ReorderItineraryCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public ReorderItineraryHandler(
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
	public ItineraryMutationResult handle(ReorderItineraryCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validateSnapshot(command);
		ensureSnapshotExists(command);

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		for (ItineraryDayOrderCommand day : command.days()) {
			repository.updateDayOrder(new ItineraryDayOrderUpdate(command.tripId(), day.dayId(), day.sortOrder(), now));
			for (ItineraryItemOrderCommand item : day.itemOrders()) {
				repository.updateItemOrder(new ItineraryItemOrderUpdate(
					command.tripId(),
					day.dayId(),
					item.itemId(),
					item.sortOrder(),
					command.actorUserId(),
					now
				));
			}
		}
		eventRepository.save(ItineraryCollaborationEvents.itineraryReordered(
			command.tripId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			command.days(),
			now
		));
		return new ItineraryMutationResult(command.tripId(), newVersion, null, null, null, List.of());
	}

	private void validateSnapshot(ReorderItineraryCommand command) {
		if (command.days().isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary order snapshot must include days.");
		}
		Set<UUID> dayIds = new HashSet<>();
		Set<UUID> itemIds = new HashSet<>();
		for (ItineraryDayOrderCommand day : command.days()) {
			if (day.dayId() == null) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day id is required.");
			}
			if (day.sortOrder() < 0) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Day sort order must be greater than or equal to 0.");
			}
			if (!dayIds.add(day.dayId())) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Duplicate day id in itinerary order snapshot.");
			}
			for (ItineraryItemOrderCommand item : day.itemOrders()) {
				if (item.itemId() == null) {
					throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Item id is required.");
				}
				if (item.sortOrder() < 0) {
					throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Item sort order must be greater than or equal to 0.");
				}
				if (!itemIds.add(item.itemId())) {
					throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Duplicate item id in itinerary order snapshot.");
				}
			}
		}
	}

	private void ensureSnapshotExists(ReorderItineraryCommand command) {
		int itemCount = 0;
		for (ItineraryDayOrderCommand day : command.days()) {
			if (!repository.existsDay(command.tripId(), day.dayId())) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found.");
			}
			for (ItineraryItemOrderCommand item : day.itemOrders()) {
				itemCount++;
				if (!repository.existsItem(command.tripId(), item.itemId())) {
					throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item was not found.");
				}
			}
		}
		if (repository.countDays(command.tripId()) != command.days().size()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary order snapshot must include all days.");
		}
		if (repository.countActiveItems(command.tripId()) != itemCount) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary order snapshot must include all active items.");
		}
	}
}
