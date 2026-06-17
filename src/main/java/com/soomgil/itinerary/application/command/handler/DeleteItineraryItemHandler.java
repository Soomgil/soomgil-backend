package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.DeleteItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteItineraryItemCommand}를 처리해 item과 연결 route를 soft delete한다.
 */
@Component
public class DeleteItineraryItemHandler implements CommandHandler<DeleteItineraryItemCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public DeleteItineraryItemHandler(
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
	public ItineraryMutationResult handle(DeleteItineraryItemCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		if (command.itemId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Item id is required.");
		}
		if (!repository.existsItem(command.tripId(), command.itemId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item was not found.");
		}

		List<UUID> affectedRouteIds = repository.findActiveRouteIdsByItem(command.tripId(), command.itemId());
		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		boolean deleted = repository.softDeleteItem(command.tripId(), command.itemId(), command.actorUserId(), now);
		if (!deleted) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item was not found.");
		}
		for (UUID routeId : affectedRouteIds) {
			repository.softDeleteRouteSegment(command.tripId(), routeId, command.actorUserId(), now);
			eventRepository.save(ItineraryCollaborationEvents.routeSegmentDeleted(
				command.tripId(),
				routeId,
				command.actorUserId(),
				command.baseVersion(),
				newVersion,
				now
			));
		}
		eventRepository.save(ItineraryCollaborationEvents.itemDeleted(
			command.tripId(),
			command.itemId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			affectedRouteIds,
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			null,
			null,
			null,
			affectedRouteIds
		);
	}
}
